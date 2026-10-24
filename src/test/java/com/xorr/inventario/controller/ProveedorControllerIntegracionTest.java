package com.xorr.inventario.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xorr.IntegracionBase;
import com.xorr.seguridad.ClienteHttp;

// Proveedores con la aplicación real: CRUD, RN-05, RN-12 (no se elimina con órdenes) y RN-14 (cascada de asociaciones).
class ProveedorControllerIntegracionTest extends IntegracionBase {

    private String token;

    @BeforeEach
    void iniciarSesionComoAdministrador() throws Exception {
        token = registrarEIniciarSesion("Ferretería Proveedores");
    }

    @Test
    void crear_conDatosValidos_responde201() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/proveedores", token,
                "{\"nombre\":\"Ferrecentro\",\"telefono\":\"3101112233\"}");

        assertEquals(201, respuesta.statusCode());
        assertEquals("Ferrecentro", ClienteHttp.texto(respuesta, "$.nombre"));
    }

    @Test
    void crear_sinNombre_responde400() throws Exception {
        assertEquals(400, http.post("/api/proveedores", token, "{\"telefono\":\"3101112233\"}").statusCode());
    }

    @Test
    void crear_siendoEmpleado_responde403() throws Exception {
        String tokenEmpleado = crearEmpleadoEIniciarSesion(token);

        assertEquals(403, http.post("/api/proveedores", tokenEmpleado, "{\"nombre\":\"No\"}").statusCode());
    }

    @Test
    void listar_incluyeElProveedorCreado() throws Exception {
        long id = crearProveedor(token);

        HttpResponse<String> lista = http.get("/api/proveedores", token);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("\"id\":" + id + ","));
    }

    @Test
    void obtener_proveedorPropio_responde200() throws Exception {
        long id = crearProveedor(token);

        assertEquals(200, http.get("/api/proveedores/" + id, token).statusCode());
    }

    @Test
    void obtener_proveedorInexistente_responde404() throws Exception {
        assertEquals(404, http.get("/api/proveedores/" + ID_INEXISTENTE, token).statusCode());
    }

    @Test
    void obtener_proveedorDeOtraEmpresa_responde404() throws Exception {
        long id = crearProveedor(token);
        String tokenOtraEmpresa = registrarEIniciarSesion("Ferretería Ajena");

        assertEquals(404, http.get("/api/proveedores/" + id, tokenOtraEmpresa).statusCode());
    }

    @Test
    void actualizar_cambiaElNombre() throws Exception {
        long id = crearProveedor(token);

        HttpResponse<String> respuesta = http.put("/api/proveedores/" + id, token,
                "{\"nombre\":\"Nombre nuevo\",\"telefono\":\"3200000000\"}");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Nombre nuevo", ClienteHttp.texto(http.get("/api/proveedores/" + id, token), "$.nombre"));
    }

    @Test
    void actualizar_sinNombre_responde400() throws Exception {
        long id = crearProveedor(token);

        assertEquals(400, http.put("/api/proveedores/" + id, token, "{\"telefono\":\"3200000000\"}").statusCode());
    }

    @Test
    void eliminar_sinMovimientos_responde204YDespuesNoExiste() throws Exception {
        long id = crearProveedor(token);

        assertEquals(204, http.delete("/api/proveedores/" + id, token).statusCode());
        assertEquals(404, http.get("/api/proveedores/" + id, token).statusCode());
    }

    @Test
    void eliminar_conOrdenesDeCompra_responde400YSigueExistiendo() throws Exception {
        long idProducto = crearProducto(token, 500, 0, 1);
        long idProveedor = crearProveedor(token);
        asociar(token, idProveedor, idProducto, 300);
        crearOrden(token, idProveedor, idProducto, 1);

        assertEquals(400, http.delete("/api/proveedores/" + idProveedor, token).statusCode());
        assertEquals(200, http.get("/api/proveedores/" + idProveedor, token).statusCode());
    }

    @Test
    void eliminar_borraEnCascadaSusAsociaciones() throws Exception {
        long idProducto = crearProducto(token, 500, 0, 1);
        long idProveedor = crearProveedor(token);
        long idAsociacion = asociar(token, idProveedor, idProducto, 300);

        assertEquals(204, http.delete("/api/proveedores/" + idProveedor, token).statusCode());
        assertEquals(404, http.get("/api/proveedor-productos/" + idAsociacion, token).statusCode());
    }
}