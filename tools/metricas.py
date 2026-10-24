#!/usr/bin/env python3
"""
metricas.py (versión recreada el 24-10-2026) · Análisis estático simple de Xorr.
NO es una herramienta estándar: es un script propio, para comparar ANTES y DESPUÉS con el mismo método.

Uso (desde la raíz del proyecto):   python tools/metricas.py src/main/java

Definiciones:
  LOC    líneas no vacías del archivo (con comentarios).
  CBO    clases DEL PROYECTO que la clase nombra (imports y usos por nombre, sin contar comentarios ni
         textos); incluye DTO, excepciones y repositorios; no cuenta a la propia clase.
  DIT    profundidad de herencia desde Object (sin 'extends' = 1).
  LCOM4  grupos de métodos conectados por campos compartidos o por llamadas entre ellos (1 = cohesiva).
         No se calcula (se muestra 1) en entidades JPA, cuyos getters y setters no comparten campos por
         diseño, ni en clases sin campos de instancia (utilidades con métodos estáticos).
  Módulos: dependencias entre paquetes com.xorr.<módulo> según los imports, y ciclos entre módulos.
"""
import re, sys, pathlib, collections

RAIZ = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/java")
DIT_EXTERNO = {"RuntimeException": 3, "Exception": 2, "OncePerRequestFilter": 2, "Throwable": 1}

def limpiar(src):
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    src = re.sub(r"//[^\n]*", " ", src)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    return src

def tipos_declarados(codigo):
    return re.findall(r"\b(?:class|interface|record|enum)\s+([A-Z]\w*)", codigo)

archivos = sorted(RAIZ.rglob("*.java"))
info = {}
for f in archivos:
    bruto = f.read_text(encoding="utf-8", errors="ignore")
    cod = limpiar(bruto)
    nombre = f.stem
    paquete = re.search(r"package\s+([\w.]+);", cod).group(1)
    info[nombre] = dict(ruta=f, bruto=bruto, cod=cod, paquete=paquete,
                        loc=sum(1 for l in bruto.splitlines() if l.strip()))
nombres = set(info)

def modulo(paquete):
    p = paquete.split(".")
    return p[2] if len(p) > 2 else "(raiz)"

# ---- CBO, DIT
for n, d in info.items():
    cuerpo = re.sub(r"^\s*(package|import)\b[^\n]*", " ", d["cod"], flags=re.M)
    usados = {t for t in nombres if t != n and re.search(r"\b" + re.escape(t) + r"\b", cuerpo)}
    imports = {m.split(".")[-1] for m in re.findall(r"^\s*import\s+(com\.xorr[\w.]*);", d["cod"], flags=re.M)}
    d["cbo_set"] = (usados | (imports & nombres)) - {n}
    m = re.search(r"\bclass\s+" + re.escape(n) + r"\b[^{]*?\bextends\s+(\w+)", d["cod"])
    padre = m.group(1) if m else None
    prof = 1
    visto = n
    while padre:
        if padre in info:
            prof += 1
            m2 = re.search(r"\bclass\s+" + re.escape(padre) + r"\b[^{]*?\bextends\s+(\w+)", info[padre]["cod"])
            padre = m2.group(1) if m2 else None
        else:
            prof += DIT_EXTERNO.get(padre, 1)
            padre = None
    d["dit"] = prof

# ---- LCOM4
def metodos_y_campos(cod, nombre):
    campos = set(re.findall(r"\b(?:private|protected|public)?\s*(?:static\s+)?(?:final\s+)?[\w<>\[\],.? ]+\s+(\w+)\s*(?:=[^;]*)?;", cod))
    campos = {c for c in re.findall(r"(?:private|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],.? ]+?\s+(\w+)\s*(?:=[^;]*)?;", cod)}
    metodos = {}
    patron = re.compile(r"(?:public|protected|private|static|final|synchronized|\s)+[\w<>\[\],.? ]+\s+(\w+)\s*\([^)]*\)\s*(?:throws[^{]*)?\{")
    for m in patron.finditer(cod):
        nom = m.group(1)
        if nom in ("if", "for", "while", "switch", "catch", "return", "new", "else", nombre):
            continue
        i = m.end(); nivel = 1
        while i < len(cod) and nivel:
            nivel += (cod[i] == "{") - (cod[i] == "}")
            i += 1
        metodos[nom] = cod[m.end():i - 1]
    return metodos, campos

for n, d in info.items():
    metodos, campos = metodos_y_campos(d["cod"], n)
    campos_inst = {c for c in re.findall(r"(?:private|protected)\s+(?!static)(?:final\s+)?[\w<>\[\],.? ]+?\s+(\w+)\s*(?:=[^;]*)?;", d["cod"])}
    if len(metodos) <= 1 or d["paquete"].endswith(".entity") or not campos_inst:
        d["lcom4"] = 1; continue
    padre = {m: m for m in metodos}
    def hallar(x):
        while padre[x] != x:
            padre[x] = padre[padre[x]]; x = padre[x]
        return x
    def unir(a, b):
        padre[hallar(a)] = hallar(b)
    nom_list = list(metodos)
    for a in nom_list:
        for c in campos:
            if re.search(r"\b" + re.escape(c) + r"\b", metodos[a]):
                for b in nom_list:
                    if b != a and re.search(r"\b" + re.escape(c) + r"\b", metodos[b]):
                        unir(a, b)
        for b in nom_list:
            if b != a and re.search(r"\b" + re.escape(b) + r"\s*\(", metodos[a]):
                unir(a, b)
    d["lcom4"] = len({hallar(m) for m in metodos})

# ---- Módulos
deps = collections.defaultdict(set)
for n, d in info.items():
    mo = modulo(d["paquete"])
    for imp in re.findall(r"^\s*import\s+com\.xorr\.(\w+)\.", d["cod"], flags=re.M):
        if imp != mo:
            deps[mo].add(imp)
total_deps = sum(len(v) for v in deps.values())
def ciclos():
    pares = set()
    for a in deps:
        for b in deps[a]:
            if a in deps.get(b, ()) and a < b:
                pares.add((a, b))
    return sorted(pares)
cic = ciclos()

# ---- Salida
print("MÉTRICAS DE DISEÑO (análisis estático propio, no herramienta estándar)")
print(f"Archivos analizados: {len(info)} | LOC no vacías: {sum(d['loc'] for d in info.values())}\n")
print(f"{'Clase':34s}{'LOC':>5s}{'CBO':>5s}{'DIT':>5s}{'LCOM4':>7s}")
for n in sorted(info, key=lambda k: (-len(info[k]['cbo_set']), k)):
    d = info[n]
    print(f"{n:34s}{d['loc']:>5d}{len(d['cbo_set']):>5d}{d['dit']:>5d}{d['lcom4']:>7d}")
serv = [n for n in info if n.endswith("Service")]
if serv:
    cb = [len(info[n]["cbo_set"]) for n in serv]
    print(f"\nServicios ({len(serv)}): CBO medio {sum(cb)/len(cb):.1f}, máximo {max(cb)}")
print(f"DIT máximo: {max(d['dit'] for d in info.values())}")
print("LCOM4 > 1: " + (", ".join(n for n, d in sorted(info.items()) if d['lcom4'] > 1) or "ninguna"))
print(f"\nDependencias entre módulos: {total_deps}")
for a in sorted(deps):
    print(f"  {a} -> {', '.join(sorted(deps[a]))}")
print(f"Ciclos entre módulos: {len(cic)}" + ("  " + "; ".join(f"{a}<->{b}" for a, b in cic) if cic else ""))