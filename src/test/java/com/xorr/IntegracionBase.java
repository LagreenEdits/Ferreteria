package com.xorr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.xorr.seguridad.ClienteHttp;

// Base de las pruebas de integración de controladores: arranca la aplicación real y ofrece
// atajos para registrar una empresa, crear un empleado y preparar productos, proveedores,
// asociaciones y órdenes. Cada prueba usa nombres y correos únicos (UUID) porque la base H2
// en memoria se comparte entre las clases de prueba.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegracionBase {

    protected static final String CLAVE = "clave12345";
    protected static final long ID_INEXISTENTE = 999_999_999L;

    @LocalServerPort
    private int puerto;

    protected ClienteHttp http;

    @BeforeEach
    protected void prepararCliente() {
        http = new ClienteHttp(puerto);
    }

    protected String registrarEIniciarSesion(String nombreEmpresa) throws Exception {
        String correo = "admin-" + UUID.randomUUID() + "@correo.com";
        String registro = "{\"nombreEmpresa\":\"" + nombreEmpresa + "\",\"nombre\":\"Admin\",\"correo\":\""
                + correo + "\",\"password\":\"" + CLAVE + "\"}";
        assertEquals(201, http.post("/api/auth/registro-admin", null, registro).statusCode());
        return iniciarSesion(correo);
    }

    protected String crearEmpleadoEIniciarSesion(String tokenAdmin) throws Exception {
        String correo = "empleado-" + UUID.randomUUID() + "@correo.com";
        String cuerpo = "{\"nombre\":\"Empleado\",\"correo\":\"" + correo + "\",\"password\":\"" + CLAVE + "\"}";
        assertEquals(201, http.post("/api/usuarios", tokenAdmin, cuerpo).statusCode());
        return iniciarSesion(correo);
    }

    private String iniciarSesion(String correo) throws Exception {
        HttpResponse<String> login = http.post("/api/auth/login", null,
                "{\"correo\":\"" + correo + "\",\"password\":\"" + CLAVE + "\"}");
        assertEquals(200, login.statusCode());
        return ClienteHttp.texto(login, "$.token");
    }

    protected String cuerpoProducto(String nombre, int precioVenta, int stock, int stockMinimo) {
        return "{\"nombre\":\"" + nombre + "\",\"precioVenta\":" + precioVenta + ",\"stock\":" + stock
                + ",\"stockMinimo\":" + stockMinimo + "}";
    }

    protected long crearProducto(String token, int precioVenta, int stock, int stockMinimo) throws Exception {
        return crearProductoConNombre(token, "Producto-" + UUID.randomUUID(), precioVenta, stock, stockMinimo);
    }

    protected long crearProductoConNombre(String token, String nombre, int precioVenta, int stock,
            int stockMinimo) throws Exception {
        HttpResponse<String> creado = http.post("/api/productos", token,
                cuerpoProducto(nombre, precioVenta, stock, stockMinimo));
        assertEquals(201, creado.statusCode());
        return ClienteHttp.numero(creado, "$.id");
    }

    protected long crearProveedor(String token) throws Exception {
        HttpResponse<String> creado = http.post("/api/proveedores", token,
                "{\"nombre\":\"Proveedor-" + UUID.randomUUID() + "\",\"telefono\":\"3000000000\"}");
        assertEquals(201, creado.statusCode());
        return ClienteHttp.numero(creado, "$.id");
    }

    protected String cuerpoAsociacion(long idProveedor, long idProducto, int precioCompra) {
        return "{\"idProveedor\":" + idProveedor + ",\"idProducto\":" + idProducto + ",\"precioCompra\":"
                + precioCompra + "}";
    }

    protected long asociar(String token, long idProveedor, long idProducto, int precioCompra) throws Exception {
        HttpResponse<String> creada = http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(idProveedor, idProducto, precioCompra));
        assertEquals(201, creada.statusCode());
        return ClienteHttp.numero(creada, "$.id");
    }

    protected String ordenConUnaLinea(long idProveedor, long idProducto, int cantidad) {
        return "{\"idProveedor\":" + idProveedor + ",\"detalles\":[{\"idProducto\":" + idProducto
                + ",\"cantidad\":" + cantidad + "}]}";
    }

    protected long crearOrden(String token, long idProveedor, long idProducto, int cantidad) throws Exception {
        HttpResponse<String> creada = http.post("/api/ordenes-compra", token,
                ordenConUnaLinea(idProveedor, idProducto, cantidad));
        assertEquals(201, creada.statusCode());
        return ClienteHttp.numero(creada, "$.id");
    }

    protected String facturaConUnaLinea(long idProducto, int cantidad) {
        return "{\"detalles\":[{\"idProducto\":" + idProducto + ",\"cantidad\":" + cantidad + "}]}";
    }

    protected long stockDe(String token, long idProducto) throws Exception {
        return ClienteHttp.numero(http.get("/api/productos/" + idProducto, token), "$.stock");
    }
}