import re, os, glob, json
import sys
root=sys.argv[1] if len(sys.argv)>1 else 'src/main/java'
files=glob.glob(root+'/**/*.java',recursive=True)
src={}
for f in files:
    t=open(f,encoding='utf-8').read()
    name=os.path.basename(f)[:-5]
    pkg=re.search(r'package ([\w.]+);',t).group(1)
    src[name]=dict(file=f,pkg=pkg,text=t)
names=set(src)
def strip(t):
    t=re.sub(r'/\*.*?\*/','',t,flags=re.S); t=re.sub(r'//[^\n]*','',t); t=re.sub(r'"(\\.|[^"\\])*"','""',t); return t
JDK_DIT={'RuntimeException':3,'OncePerRequestFilter':2,'Exception':2}
def dit(n,seen=()):
    t=strip(src[n]['text'])
    m=re.search(r'\b(?:class|record|enum|interface)\s+'+n+r'\b[^{]*?\bextends\s+(\w+)',t,flags=re.S)
    if not m: return 1
    p=m.group(1)
    if p in src: return dit(p)+1
    return JDK_DIT.get(p,1)+1
def kind(n):
    pk=src[n]['pkg']; t=src[n]['text']
    for k in ('controller','service','repository','entity','dto','config'):
        if pk.endswith('.'+k): return k
    return 'common' if pk.endswith('common') else pk.split('.')[-1]
rows=[]
for n,d in sorted(src.items()):
    t=strip(d['text'])
    body=re.sub(r'^\s*(package|import)[^\n]*\n','',t,flags=re.M)
    used={o for o in names if o!=n and re.search(r'\b'+o+r'\b',body)}
    cbo=len(used)
    # LCOM4
    fields=re.findall(r'private\s+(?:final\s+)?(?:static\s+)?[\w<>\[\], ?]+?\s+(\w+)\s*(?:=[^;]*)?;',body)
    fields=[f for f in fields if not re.search(r'static\s+final[^;]*\b'+f+r'\b',body)]
    meths=[]
    for m in re.finditer(r'\n\s*(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\], ?]+\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [\w, ]+)?\{',body):
        meths.append((m.group(1),m.start()))
    comps=None;nm=len(meths)
    k=kind(n)
    if k in('service','controller','config','common') and nm>0 and 'record ' not in body[:300]:
        # extraer cuerpos por llaves
        bodies={}
        for nm_,st in meths:
            i=body.index('{',st); dep=0;j=i
            while True:
                if body[j]=='{':dep+=1
                elif body[j]=='}':
                    dep-=1
                    if dep==0:break
                j+=1
            bodies[nm_+str(st)]=(nm_,body[i:j+1])
        keys=list(bodies)
        parent={x:x for x in keys}
        def f(x):
            while parent[x]!=x: parent[x]=parent[parent[x]];x=parent[x]
            return x
        for a in keys:
            for b in keys:
                if a>=b: continue
                A=bodies[a][1];B=bodies[b][1]
                share=any(re.search(r'\b'+fl+r'\b',A) and re.search(r'\b'+fl+r'\b',B) for fl in fields)
                call=re.search(r'\b'+bodies[b][0]+r'\s*\(',A) or re.search(r'\b'+bodies[a][0]+r'\s*\(',B)
                if share or call: parent[f(a)]=f(b)
        comps=len({f(x) for x in keys})
    loc=len([l for l in d['text'].splitlines() if l.strip()])
    rows.append((d['pkg'].replace('com.xorr.',''),n,k,loc,cbo,dit(n),comps,sorted(used)))
json.dump(rows,open('metricas.json','w'),ensure_ascii=False)
print(f"{'paquete':22}{'clase':30}{'LOC':>4}{'CBO':>4}{'DIT':>4}{'LCOM4':>6}")
for r in rows:
    if r[2] in('service','controller','config','common'):
        print(f"{r[0]:22}{r[1]:30}{r[3]:>4}{r[4]:>4}{r[5]:>4}{str(r[6] if r[6] is not None else '-'):>6}")
