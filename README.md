# Xorr · Sistema Gestor de Ferreterías

Xorr es una aplicación para que las **microferreterías del Tolima** controlen su **inventario**, sus **proveedores** y sus **órdenes de compra** (y, próximamente, las **facturas de venta**). Está pensada para que varias ferreterías usen el mismo sistema sin ver los datos de las demás.

Es un proyecto académico del curso de **Calidad de Software** (IDEAD, Universidad del Tolima), construido por un equipo de tres desarrolladores. Por eso se construye **por fases, de forma simple y con pruebas**: primero lo básico funcionando, después lo demás.

> **¿Eres nuevo programando?** Este README está escrito para ti. Lee las secciones en orden; si aparece una palabra desconocida, búscala en el [Glosario](#17-glosario-para-principiantes) del final.

---

## Tabla de contenido

1. [Estado actual del proyecto](#1-estado-actual-del-proyecto)
2. [¿Qué es esto y cómo funciona? (contexto para principiantes)](#2-qué-es-esto-y-cómo-funciona-contexto-para-principiantes)
3. [Tecnologías usadas](#3-tecnologías-usadas)
4. [Qué necesitas instalar](#4-qué-necesitas-instalar)
5. [Cómo descargar el proyecto](#5-cómo-descargar-el-proyecto)
6. [Cómo ejecutarlo](#6-cómo-ejecutarlo)
7. [Probar la API paso a paso](#7-probar-la-api-paso-a-paso)
8. [Estructura de archivos y rutas](#8-estructura-de-archivos-y-rutas)
9. [Endpoints disponibles](#9-endpoints-disponibles)
10. [Cómo está organizado el código](#10-cómo-está-organizado-el-código)
11. [Reglas de negocio](#11-reglas-de-negocio)
12. [Pruebas automáticas](#12-pruebas-automáticas)
13. [Formato de los errores](#13-formato-de-los-errores)
14. [Problemas frecuentes](#14-problemas-frecuentes)
15. [Qué falta por hacer](#15-qué-falta-por-hacer)
16. [Cómo colaborar](#16-cómo-colaborar)
17. [Glosario para principiantes](#17-glosario-para-principiantes)

---

## 1. Estado actual del proyecto

**Fase actual: Fase 0 (versión mínima).** Solo existe el **backend** (la parte que guarda datos y aplica las reglas). **No hay pantallas todavía**: se prueba con Swagger, una página que genera el propio proyecto.

| Módulo | Estado | Qué incluye |
|---|---|---|
| **Inventario · Productos** | ✅ Listo | Crear, listar, ver, actualizar y eliminar productos; listar los de **stock bajo**; subir y bajar stock con validaciones |
| **Inventario · Proveedores** | ✅ Listo | Crear, listar, ver, actualizar y eliminar proveedores |
| **Inventario · Proveedor-Producto** | ✅ Listo | Asociar un producto a un proveedor con su **precio de compra** |
| **Compras** | ✅ Listo | Crear una orden de compra con varias líneas, **recibirla** (sube el stock), **cancelarla**, consultarla y listarla |
| **Regla "no borrar con movimientos"** | ✅ Listo | No deja eliminar un producto o proveedor que ya tiene órdenes de compra |
| **Ventas (facturas)** | 🔜 Siguiente | Vender descontando stock |
| **Seguridad (login y roles)** | 🔜 Pendiente | Hoy **no hay login**: todo se hace como la empresa 1 y el usuario 1 |
| **Pantallas web (frontend)** | 🔜 Pendiente | Páginas HTML + JavaScript |
| **Reportes, PDF, clientes** | 🔜 Fase 2 | |

**Pruebas automáticas:** 55 pruebas, todas en verde (`BUILD SUCCESS`).

**Base de datos:** H2 en memoria. Es una base de datos temporal que **se borra cada vez que apagas la aplicación**. MySQL ya está incluido como dependencia, pero todavía no está configurado.

---

## 2. ¿Qué es esto y cómo funciona? (contexto para principiantes)

### Lo básico en una imagen mental

Imagina un restaurante:

- El **cliente** (tú, o en el futuro una página web) hace un **pedido**.
- El **mesero** (el *controlador*) recibe el pedido y lo lleva a la cocina.
- La **cocina** (el *servicio*) decide si el pedido se puede preparar y sigue las reglas de la casa.
- La **despensa** (el *repositorio*) guarda y entrega los ingredientes (los datos).

Xorr funciona igual. Es una **API REST**: un programa que espera "pedidos" por internet (o por tu propio computador) en direcciones como `/api/productos`, y responde con datos en formato **JSON**.

```
Tú (Swagger, Postman o una página web)
      │  petición HTTP, por ejemplo: POST /api/productos
      ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  Controller  │──▶│   Service    │──▶│  Repository  │──▶│ Base de datos │
│ (recibe)     │   │ (reglas)     │   │ (guarda)     │   │ (H2 / MySQL)  │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
      ▲                                                          
      └── respuesta en JSON (por ejemplo: 201 Created + datos del producto)
```

### Conceptos que vas a ver en el código

| Concepto | En palabras simples | Ejemplo en este proyecto |
|---|---|---|
| **Entity (entidad)** | Una clase que representa una tabla de la base de datos | `Producto`, `OrdenCompra` |
| **DTO** | Una clase con solo los datos que viajan por la API (nunca se expone la entidad directamente) | `ProductoRequest` (lo que envías), `ProductoResponse` (lo que recibes) |
| **Repository** | Una interfaz que sabe guardar y buscar entidades | `ProductoRepository` |
| **Service** | Donde viven las **reglas de negocio** | `OrdenCompraService` |
| **Controller** | Define las direcciones (`/api/...`) y llama al servicio. No decide nada | `OrdenCompraController` |
| **Multi-tenant** | Varias empresas usan el sistema sin ver los datos de las otras | Toda tabla lleva `id_empresa` |
| **Transacción** | "Todo o nada": si algo falla a la mitad, no se guarda nada | `recibir` una orden |

### Un ejemplo de punta a punta: recibir una orden de compra

Cuando llega mercancía, el dueño marca la orden como **RECIBIDA** y el stock sube:

1. Haces `PUT /api/ordenes-compra/1/recibir`.
2. `OrdenCompraController` recibe la petición y llama a `OrdenCompraService.recibir(1)`.
3. El servicio busca la orden **de tu empresa** (si es de otra empresa, responde "no encontrada").
4. Comprueba que la orden esté `PENDIENTE` (una orden ya recibida o cancelada no se puede volver a recibir).
5. Por cada línea de la orden, llama a `ProductoService.incrementarStock(...)`.
6. Cambia el estado a `RECIBIDA` y guarda.
7. Si **cualquier línea falla**, la transacción se revierte y **ningún stock cambia**.

---

## 3. Tecnologías usadas

| Tecnología | Versión | Para qué sirve |
|---|---|---|
| **Java** | 17 (objetivo) | El lenguaje de programación |
| **Spring Boot** | 3.3.5 | Framework que arma la aplicación web y la API |
| **Maven** | 3.9+ | Descarga las librerías y compila, prueba y ejecuta el proyecto (lee el archivo `pom.xml`) |
| **Spring Data JPA** | (incluido) | Permite guardar y leer datos sin escribir SQL a mano |
| **H2** | (incluido) | Base de datos en memoria para desarrollo |
| **MySQL Connector** | (incluido) | Para usar MySQL más adelante (aún sin configurar) |
| **Bean Validation** | (incluido) | Valida los datos de entrada (campos obligatorios, números no negativos...) |
| **springdoc-openapi** | 2.6.0 | Genera **Swagger**, la página para probar la API |
| **JUnit 5 + Mockito** | (incluido) | Pruebas automáticas |

> **Dato técnico:** el proyecto compila para Java 17, pero se ha probado con **JDK 25**. Para que Mockito funcione en JDK 25, el `pom.xml` fuerza la versión `1.17.7` de Byte Buddy (propiedad `byte-buddy.version`). No la quites.

---

## 4. Qué necesitas instalar

| Programa | Para qué | Cómo comprobar que ya lo tienes |
|---|---|---|
| **JDK (Java 17 o superior)** | Compilar y ejecutar | `java -version` |
| **Maven 3.9 o superior** | Construir y probar | `mvn -v` |
| **Git** | Descargar el código y subir cambios | `git --version` |
| Un editor (recomendado: **VS Code** con el *Extension Pack for Java*, o **IntelliJ IDEA**) | Leer y editar el código | — |
| **Postman** (opcional) | Probar la API más cómodo que con Swagger | — |

Abre una terminal (en Windows: `cmd`, PowerShell o la terminal de VS Code) y escribe los tres comandos de la columna derecha. Si alguno dice que "no se reconoce", todavía no está instalado o no está en el `PATH` (mira [Problemas frecuentes](#14-problemas-frecuentes)).

**No necesitas** instalar ninguna base de datos: H2 viene incluida y se crea sola al arrancar.

**No hay Maven Wrapper** (`mvnw`) en el repositorio, así que necesitas Maven instalado en tu computador.

---

## 5. Cómo descargar el proyecto

### Opción A · Con Git (recomendada)

```bash
git clone https://github.com/LagreenEdits/Ferreteria.git
cd Ferreteria
```

El repositorio se llama **Ferreteria**, así que la carpeta que se crea también se llamará `Ferreteria` (el proyecto por dentro se llama **Xorr**). La rama principal es `main`.

### Opción B · Sin Git (ZIP)

1. En la página del repositorio en GitHub pulsa el botón verde **Code**.
2. Elige **Download ZIP**.
3. Descomprime el archivo y abre una terminal dentro de la carpeta descomprimida (la que contiene `pom.xml`).

> **Todos los comandos de este README se ejecutan dentro de la carpeta que contiene el archivo `pom.xml`.** Si Maven dice que no encuentra un `pom.xml`, estás en la carpeta equivocada.

---

## 6. Cómo ejecutarlo

### 6.1 Correr las pruebas (comprobar que todo está bien)

```bash
mvn test
```

La primera vez tarda más porque Maven descarga las librerías (necesitas internet). Al final debes ver:

```
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Verás varios mensajes `WARNING` sobre Java, `Unsafe` o un agente de Byte Buddy. **Son inofensivos**: los producen las versiones recientes de Java con Maven y Mockito, no tu código.

### 6.2 Arrancar la aplicación

```bash
mvn spring-boot:run
```

Espera a ver una línea parecida a `Started XorrApplication in ... seconds`. La aplicación queda corriendo en el **puerto 8081**.

| Qué | Dirección |
|---|---|
| **Swagger** (probar la API desde el navegador) | http://localhost:8081/swagger-ui.html |
| **Consola de H2** (ver las tablas y datos) | http://localhost:8081/h2-console |
| La API | http://localhost:8081/api/... |

Para entrar a la **consola de H2** usa estos datos:

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:xorr` |
| User Name | `sa` |
| Password | *(vacía)* |

### 6.3 Apagar la aplicación

En la terminal donde la arrancaste pulsa **Ctrl + C**. Recuerda: al apagar, **se borran todos los datos** (la base es en memoria).

### 6.4 Cambiar el puerto

Está en `src/main/resources/application-dev.properties`:

```properties
server.port=8081
```

Cámbialo (por ejemplo a `8080`) si ese puerto ya lo usa otro programa.

---

## 7. Probar la API paso a paso

Con la aplicación arrancada, abre **Swagger** (http://localhost:8081/swagger-ui.html). Verás un grupo de endpoints por cada controlador. Para probar uno: ábrelo, pulsa **Try it out**, escribe el cuerpo (si lo pide) y pulsa **Execute**.

Este recorrido sirve para entender todo el sistema. Hazlo **en orden** y con la base recién arrancada (los números `id` empiezan en 1).

**Paso 1 · Crear un producto** → `POST /api/productos`

```json
{
  "nombre": "Martillo",
  "precioVenta": 15000,
  "stock": 2,
  "stockMinimo": 5
}
```
Respuesta: `201`. Verás el producto con su `id` (será `1`).

**Paso 2 · Ver el stock bajo** → `GET /api/productos/stock-bajo`

El martillo aparece porque tiene `2` y el mínimo es `5` (stock **menor** que el mínimo).

**Paso 3 · Crear un proveedor** → `POST /api/proveedores`

```json
{
  "nombre": "Ferrelectro",
  "telefono": "3001234567"
}
```

**Paso 4 · Asociar el producto al proveedor con su precio de compra** → `POST /api/proveedor-productos`

```json
{
  "idProveedor": 1,
  "idProducto": 1,
  "precioCompra": 9000
}
```
Si lo repites, responde `400` con `"Ya existe esa asociación"`.

**Paso 5 · Crear una orden de compra** → `POST /api/ordenes-compra`

```json
{
  "idProveedor": 1,
  "detalles": [
    { "idProducto": 1, "cantidad": 10 }
  ]
}
```
Fíjate: **no envías precios**. El servidor toma el precio de la asociación (9000), calcula el subtotal (90000) y el total. La orden queda `PENDIENTE`.

**Paso 6 · Recibir la orden** → `PUT /api/ordenes-compra/1/recibir`

La orden pasa a `RECIBIDA`. Comprueba con `GET /api/productos/1` que el stock ahora es `12`, y con `GET /api/productos/stock-bajo` que el martillo **ya no aparece**.

**Paso 7 · Probar las protecciones**

| Qué haces | Qué debe pasar |
|---|---|
| `PUT /api/ordenes-compra/1/recibir` otra vez | `400` · `No se puede recibir una orden en estado RECIBIDA` |
| `PUT /api/ordenes-compra/1/cancelar` | `400` · una orden recibida no se cancela |
| `DELETE /api/productos/1` | `400` · `No se puede eliminar el producto 'Martillo' porque tiene órdenes de compra` |
| `DELETE /api/proveedores/1` | `400` · el proveedor también tiene órdenes |
| `GET /api/productos/999` | `404` · producto no encontrado |
| Crear una orden con `"cantidad": 0` | `400` · `detalles[0].cantidad: La cantidad debe ser mayor que cero` |

---

## 8. Estructura de archivos y rutas

Todas las rutas son relativas a la raíz del proyecto (donde está `pom.xml`).

```
Xorr/
├── pom.xml                          ← Configuración de Maven: dependencias y versiones
├── .gitignore                       ← Archivos que Git no sube (target/, .idea/, .vscode/, .env...)
├── README.md                        ← Este archivo
├── .github/modernize/               ← Lo generó una herramienta de VS Code (actualización de Java); no es parte del proyecto
│
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.properties        ← Activa el perfil "dev"
    │   │   └── application-dev.properties    ← Puerto 8081, base H2 y consola H2
    │   │
    │   └── java/com/xorr/
    │       ├── XorrApplication.java          ← Punto de entrada: aquí arranca todo
    │       │
    │       ├── common/                       ← Piezas compartidas por todos los módulos
    │       │   ├── ContextoActual.java            ← Da el idEmpresa y el idUsuario (hoy fijos en 1)
    │       │   ├── GlobalExceptionHandler.java    ← Convierte los errores en respuestas JSON
    │       │   ├── RecursoNoEncontradoException.java   ← Error 404
    │       │   ├── OperacionInvalidaException.java     ← Error 400
    │       │   └── StockInsuficienteException.java     ← Error 409
    │       │
    │       ├── inventario/                   ← Productos, proveedores y su relación
    │       │   ├── controller/
    │       │   │   ├── ProductoController.java
    │       │   │   ├── ProveedorController.java
    │       │   │   └── ProveedorProductoController.java
    │       │   ├── service/
    │       │   │   ├── ProductoService.java            ← Reglas de stock (RN-01, RN-04, RN-07, RN-12)
    │       │   │   ├── ProveedorService.java
    │       │   │   └── ProveedorProductoService.java   ← Asociaciones y precio de compra
    │       │   ├── repository/
    │       │   │   ├── ProductoRepository.java
    │       │   │   ├── ProveedorRepository.java
    │       │   │   └── ProveedorProductoRepository.java
    │       │   ├── entity/
    │       │   │   ├── Producto.java
    │       │   │   ├── Proveedor.java
    │       │   │   └── ProveedorProducto.java
    │       │   └── dto/
    │       │       ├── ProductoRequest.java / ProductoResponse.java
    │       │       ├── ProveedorRequest.java / ProveedorResponse.java
    │       │       └── ProveedorProductoRequest.java / ProveedorProductoResponse.java
    │       │
    │       └── compras/                      ← Órdenes de compra
    │           ├── controller/
    │           │   └── OrdenCompraController.java
    │           ├── service/
    │           │   ├── OrdenCompraService.java         ← Crear, recibir, cancelar, consultar
    │           │   └── MovimientosCompraService.java   ← Consulta de solo lectura que usa Inventario (RN-12)
    │           ├── repository/
    │           │   └── OrdenCompraRepository.java
    │           ├── entity/
    │           │   ├── OrdenCompra.java
    │           │   ├── DetalleOrdenCompra.java
    │           │   └── EstadoOrdenCompra.java          ← PENDIENTE, RECIBIDA, CANCELADA
    │           └── dto/
    │               ├── OrdenCompraRequest.java / OrdenCompraResponse.java
    │               └── DetalleOrdenCompraRequest.java / DetalleOrdenCompraResponse.java
    │
    └── test/java/com/xorr/                   ← Pruebas automáticas (misma estructura de paquetes)
        ├── compras/service/
        │   ├── OrdenCompraServiceTest.java           ← 14 pruebas
        │   └── MovimientosCompraServiceTest.java     ← 4 pruebas
        └── inventario/service/
            ├── ProductoServiceTest.java              ← 18 pruebas
            ├── ProveedorServiceTest.java             ← 8 pruebas
            └── ProveedorProductoServiceTest.java     ← 11 pruebas
```

**Carpetas que aparecen al compilar y que no se suben a GitHub:** `target/` (el código compilado y los reportes de pruebas). Si la borras, Maven la vuelve a crear.

**Cómo ubicarte rápido:** cada módulo (`inventario`, `compras`) tiene las mismas cuatro/cinco carpetas: `controller`, `service`, `repository`, `entity`, `dto`. Si buscas "qué pasa cuando creo un producto", empieza en `inventario/controller/ProductoController.java` y sigue hacia `service`.

---

## 9. Endpoints disponibles

Prefijo de todas las rutas: `http://localhost:8081`. Hoy **cualquiera** puede llamarlas (no hay login).

### Productos · `/api/productos`

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| `POST` | `/api/productos` | Crea un producto | `201` |
| `GET` | `/api/productos` | Lista los productos de la empresa | `200` |
| `GET` | `/api/productos/stock-bajo` | Lista los productos con `stock < stockMinimo` | `200` |
| `GET` | `/api/productos/{id}` | Ve un producto | `200` / `404` |
| `PUT` | `/api/productos/{id}` | Actualiza un producto | `200` / `404` |
| `DELETE` | `/api/productos/{id}` | Elimina un producto (y sus asociaciones) | `204` / `400` / `404` |

Cuerpo de `POST` y `PUT`: `nombre` (obligatorio), `precioVenta` (obligatorio, ≥ 0), `stock` (obligatorio, ≥ 0), `stockMinimo` (obligatorio, ≥ 0).

### Proveedores · `/api/proveedores`

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| `POST` | `/api/proveedores` | Crea un proveedor | `201` |
| `GET` | `/api/proveedores` | Lista los proveedores | `200` |
| `GET` | `/api/proveedores/{id}` | Ve un proveedor | `200` / `404` |
| `PUT` | `/api/proveedores/{id}` | Actualiza un proveedor | `200` / `404` |
| `DELETE` | `/api/proveedores/{id}` | Elimina un proveedor (y sus asociaciones) | `204` / `400` / `404` |

Cuerpo: `nombre` (obligatorio) y `telefono` (opcional).

### Asociaciones proveedor-producto · `/api/proveedor-productos`

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| `POST` | `/api/proveedor-productos` | Asocia un producto a un proveedor con precio de compra | `201` / `400` / `404` |
| `GET` | `/api/proveedor-productos` | Lista las asociaciones | `200` |
| `GET` | `/api/proveedor-productos/{id}` | Ve una asociación | `200` / `404` |
| `PUT` | `/api/proveedor-productos/{id}` | Cambia proveedor, producto o precio | `200` / `400` / `404` |
| `DELETE` | `/api/proveedor-productos/{id}` | Elimina la asociación | `204` / `404` |

Cuerpo: `idProveedor` (obligatorio), `idProducto` (obligatorio), `precioCompra` (obligatorio, ≥ 0).

### Órdenes de compra · `/api/ordenes-compra`

| Método | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| `POST` | `/api/ordenes-compra` | Crea una orden `PENDIENTE` | `201` / `400` / `404` |
| `GET` | `/api/ordenes-compra` | Lista las órdenes | `200` |
| `GET` | `/api/ordenes-compra/{id}` | Ve una orden con sus líneas | `200` / `404` |
| `PUT` | `/api/ordenes-compra/{id}/recibir` | Marca la orden como `RECIBIDA` y sube el stock | `200` / `400` / `404` |
| `PUT` | `/api/ordenes-compra/{id}/cancelar` | Marca la orden como `CANCELADA` (no toca el stock) | `200` / `400` / `404` |

Cuerpo de `POST`: `idProveedor` (obligatorio) y `detalles` (lista con al menos una línea). Cada línea: `idProducto` (obligatorio) y `cantidad` (obligatoria, mínimo 1).

---

## 10. Cómo está organizado el código

### Capas y quién puede hablar con quién

| Capa | Hace | **No** puede hacer |
|---|---|---|
| `controller` | Recibe la petición y devuelve la respuesta | Llamar repositorios ni decidir reglas |
| `service` | Aplica las reglas de negocio | Hablar directo con repositorios de **otro** módulo |
| `repository` | Guarda y busca datos | Contener reglas |
| `entity` | Representa una tabla | — |
| `dto` | Define qué datos entran y salen por la API | — |

Reglas de convivencia entre módulos:

- La API **siempre** usa DTO, **nunca** entidades.
- Un servicio usa **solo los repositorios de su módulo**. Si necesita datos de otro módulo, llama al **servicio** de ese módulo (por ejemplo, `OrdenCompraService` llama a `ProductoService`, no a `ProductoRepository`).
- Dependencias permitidas: **Compras → Inventario**, y **Inventario → Compras** solo a través de `MovimientosCompraService` (consulta de lectura para impedir borrar productos o proveedores con órdenes). Esta última es una excepción justificada que se corregirá en la Fase 2.

### El "contexto actual"

`ContextoActual` es **la única clase** que dice a qué empresa y a qué usuario pertenece cada petición. Hoy devuelve siempre `1` y `1`. Cuando se agregue el login con JWT, solo cambiará esa clase y el resto del código seguirá igual. Por eso **ningún servicio recibe el `idEmpresa` desde fuera**: así un usuario nunca puede pedir datos de otra empresa.

### Estilo de código que sigue el proyecto

- **Simple primero:** no se agregan capas ni librerías que no hagan falta.
- Métodos pequeños que hacen una sola cosa, con nombres que explican su intención.
- Comentarios solo para explicar el **porqué** (por ejemplo, qué regla de negocio se aplica).
- Sin `catch` vacíos ni devolver `null` (se usa `Optional` y excepciones con mensaje claro).
- Complejidad baja: ningún método debería tener más de 10 caminos de decisión.

---

## 11. Reglas de negocio

Las reglas viven en los **servicios** y están probadas con pruebas automáticas.

| Código | Regla | Estado |
|---|---|---|
| RN-01 | No se vende sin stock suficiente (se valida solo en `decrementarStock`) | ✅ (aún sin endpoint de ventas) |
| RN-02 | Al recibir una orden de compra, sube el stock de sus productos | ✅ |
| RN-03 | Subtotales y totales los calcula el servidor, nunca el cliente | ✅ en compras |
| RN-04 | "Stock bajo" = `stock < stockMinimo` (estricto) | ✅ |
| RN-05 | Multi-tenant: un recurso de otra empresa se trata como "no encontrado" | ✅ |
| RN-06 | Recibir una orden es atómico: si falla una línea, no se guarda nada | ✅ |
| RN-07 | Cantidades mayores que 0; precios y stock nunca negativos | ✅ |
| RN-08 | Una orden solo se recibe o se cancela estando `PENDIENTE`; la recepción no se revierte | ✅ |
| RN-09 | En una orden solo van productos asociados al proveedor, al precio de esa asociación | ✅ |
| RN-10 | En una factura el precio sale de `Producto.precioVenta` | 🔜 Ventas |
| RN-11 | No hay anulación ni borrado de facturas en el MVP | 🔜 Ventas |
| RN-12 | No se elimina un producto o proveedor con movimientos | ✅ órdenes de compra · 🔜 facturas |
| RN-13 | No puede haber dos asociaciones con el mismo proveedor y producto | ✅ |
| RN-14 | Al eliminar un producto o proveedor se eliminan sus asociaciones | ✅ |

---

## 12. Pruebas automáticas

### ¿Qué es una prueba automática?

Es código que **comprueba que otro código funciona**, sin que tengas que probarlo a mano cada vez. Si cambias algo y rompes una regla, la prueba falla y te avisa.

Estas son **pruebas unitarias de los servicios**: usan **JUnit 5** (para escribir las pruebas) y **Mockito** (para crear "dobles" de los repositorios, de modo que **no necesitan base de datos**). Son rápidas: toda la suite corre en pocos segundos.

| Clase de prueba | Pruebas | Qué cubre |
|---|---|---|
| `OrdenCompraServiceTest` | 14 | Crear, recibir, cancelar, consultar; precios y totales; estados; multi-tenant |
| `MovimientosCompraServiceTest` | 4 | Consulta de órdenes por producto y por proveedor |
| `ProductoServiceTest` | 18 | Subir y bajar stock, cantidades inválidas, stock bajo, eliminar, multi-tenant |
| `ProveedorServiceTest` | 8 | CRUD, eliminar con y sin órdenes, multi-tenant |
| `ProveedorProductoServiceTest` | 11 | Asociaciones, duplicados, precio de compra |
| **Total** | **55** | |

### Comandos útiles

```bash
mvn test                                        # corre todas las pruebas
mvn test -Dtest=OrdenCompraServiceTest          # corre una sola clase
mvn test -Dtest=OrdenCompraServiceTest#cancelar_ordenPendiente_quedaCanceladaSinTocarElStock   # corre una sola prueba
```

Los reportes detallados quedan en `target/surefire-reports/`.

### Lo que las pruebas **no** cubren (y se comprueba a mano en Swagger)

Como no usan base de datos real, no ejecutan: las consultas de los repositorios (por ejemplo, el `<` estricto del stock bajo), la validación de los DTO (campos obligatorios, números negativos) ni el "deshacer" (rollback) de las transacciones. Por eso el recorrido de la [sección 7](#7-probar-la-api-paso-a-paso) es parte de las comprobaciones del proyecto.

### Si escribes pruebas con Mockito

Mockito es estricto: si el código llama a un método simulado con argumentos distintos a los que declaraste, la prueba falla con `PotentialStubbingProblem`. Declara el comportamiento de **cada** llamada que tu código va a hacer.

---

## 13. Formato de los errores

Todos los errores devuelven JSON con un solo campo `mensaje`:

```json
{ "mensaje": "Producto no encontrado: 999" }
```

| Código HTTP | Cuándo | Ejemplo |
|---|---|---|
| `400` Bad Request | Datos inválidos o regla de negocio incumplida | `La cantidad debe ser mayor que cero` |
| `404` Not Found | El recurso no existe **o es de otra empresa** | `Orden de compra no encontrada: 5` |
| `409` Conflict | Stock insuficiente | `Stock insuficiente para 'Martillo': disponible 2, solicitado 5` |

Cuando falla la validación de varios campos a la vez, el mensaje los junta: `nombre: El nombre es obligatorio; stock: El stock no puede ser negativo`.

---

## 14. Problemas frecuentes

| Problema | Causa probable | Solución |
|---|---|---|
| `'mvn' no se reconoce como un comando` | Maven no está instalado o no está en el `PATH` | Instala Maven 3.9+ y agrega su carpeta `bin` al `PATH`; abre una terminal **nueva** |
| `'java' no se reconoce...` o versión menor a 17 | JDK no instalado o muy antiguo | Instala un JDK 17 o superior y revisa `java -version` |
| `there is no POM in this directory` | Estás en la carpeta equivocada | Entra a la carpeta que contiene `pom.xml` |
| `Port 8081 was already in use` | Otro programa usa el puerto (o tienes la app corriendo en otra terminal) | Cierra el otro programa o cambia `server.port` en `application-dev.properties` |
| Los datos desaparecieron | La base H2 es en memoria | Es normal: se borra al apagar. Vuelve a crear los datos |
| Muchos `WARNING` en `mvn test` | Avisos de Java, Maven y Mockito | Son inofensivos; mira que termine en `BUILD SUCCESS` |
| Las pruebas fallan con un error de Byte Buddy o de Mockito en un JDK muy nuevo | Mockito no conoce esa versión de Java | Conserva `byte-buddy.version` del `pom.xml` o usa JDK 17 o 21 |
| Swagger no abre | La aplicación no está corriendo | Revisa que `mvn spring-boot:run` siga activo y entra a `http://localhost:8081/swagger-ui.html` |
| Falló la descarga de dependencias | Sin internet o red bloqueada | Reintenta con conexión; Maven guarda lo descargado en `~/.m2` |

---

## 15. Qué falta por hacer

| Fase | Contenido | Estado |
|---|---|---|
| **Fase 0** | Inventario y Compras en el backend, sin login | ✅ Hecho |
| **Fase 0 (resto)** | **Ventas (facturas)**, colección de Postman, medición de complejidad y cobertura de pruebas (JaCoCo) | 🔜 Siguiente |
| **Fase 1 · MVP** | Registro y login con JWT, roles ADMIN y EMPLEADO, empresa; páginas HTML + JavaScript | Pendiente |
| **Fase 2** | Reportes, PDF de facturas y órdenes, clientes, logo, pruebas de integración y refactor de inversión de dependencias | Futuro |
| **Fase 3** | Permisos por rol en la interfaz, HTTPS, instalador, despliegue en la nube, modo sin conexión, React, app móvil | Futuro |

**Limitaciones conocidas hoy:** no hay login ni roles; todo opera como empresa `1`, usuario `1`; la base de datos es temporal; todavía no existe el módulo de ventas.

---

## 16. Cómo colaborar

1. **Antes de empezar,** trae los últimos cambios: `git pull`.
2. **Crea una rama** para tu trabajo: `git checkout -b mi-cambio`.
3. Haz tus cambios, **pequeños y con sentido**.
4. **Corre las pruebas:** `mvn test`. Deben seguir en verde.
5. Guarda tus cambios:
   ```bash
   git add .
   git commit -m "Describe qué hiciste"
   git push -u origin mi-cambio
   ```
6. Abre un **Pull Request** en GitHub para que otro integrante lo revise.

Reglas de oro: no mezcles lógica de negocio en los controladores, no uses entidades en la API, no agregues librerías sin comentarlo con el equipo, y **toda regla nueva lleva su prueba**.

---

## 17. Glosario para principiantes

| Término | Significado |
|---|---|
| **API** | Un programa que otros programas pueden usar enviándole peticiones |
| **REST** | Un estilo de API que usa las direcciones web y los verbos HTTP (`GET`, `POST`, `PUT`, `DELETE`) |
| **Endpoint** | Una dirección concreta de la API, por ejemplo `/api/productos` |
| **HTTP** | El idioma con el que se comunican navegadores y servidores |
| **JSON** | Formato de texto para enviar datos, como `{"nombre": "Martillo"}` |
| **Código de estado** | El número que dice cómo salió una petición: `200` bien, `201` creado, `204` sin contenido, `400` mala petición, `404` no existe, `409` conflicto |
| **Backend / Frontend** | Backend: la parte que guarda datos y aplica reglas. Frontend: las pantallas que usa la persona |
| **Spring Boot** | Un framework de Java que arma una aplicación web con muy poca configuración |
| **Maven** | Herramienta que descarga librerías y compila, prueba y ejecuta el proyecto |
| **`pom.xml`** | El archivo donde Maven lee qué librerías y versiones usa el proyecto |
| **Dependencia** | Una librería externa que el proyecto necesita |
| **Entidad** | Una clase de Java que representa una tabla de la base de datos |
| **DTO** | Clase que define los datos que entran y salen por la API |
| **Repositorio** | Clase que guarda y consulta datos |
| **Servicio** | Clase donde están las reglas de negocio |
| **Controlador** | Clase que recibe las peticiones HTTP |
| **Regla de negocio** | Una norma del negocio, como "no se vende sin stock" |
| **Multi-tenant** | Varias empresas comparten el sistema sin ver los datos de las otras |
| **Transacción** | "Todo o nada": si algo falla, se deshace todo lo hecho |
| **Rollback** | Deshacer una transacción |
| **H2** | Base de datos que vive en la memoria y se borra al apagar |
| **MySQL** | Base de datos que guarda los datos de forma permanente |
| **JPA** | Tecnología de Java para guardar objetos en la base sin escribir SQL a mano |
| **Swagger** | Página generada por el proyecto para probar la API desde el navegador |
| **Postman** | Programa para enviar peticiones a una API |
| **Prueba unitaria** | Código que comprueba que una pieza pequeña funciona bien |
| **Mock (Mockito)** | Un "doble" de una clase, usado en pruebas para no depender de la base de datos |
| **JUnit** | Librería para escribir pruebas en Java |
| **`BigDecimal`** | Tipo de Java para dinero, que evita errores de redondeo |
| **Stock** | Cantidad de un producto que hay disponible |
| **`PATH`** | Lista de carpetas donde el sistema busca los programas que escribes en la terminal |
| **Git / GitHub** | Git guarda el historial de cambios; GitHub aloja el repositorio en internet |
| **Rama (branch)** | Una línea de trabajo separada, para no dañar la principal |
| **Pull Request** | Una propuesta de cambios para que el equipo la revise antes de unirla |

---

*Proyecto académico · Calidad de Software · IDEAD, Universidad del Tolima.*
