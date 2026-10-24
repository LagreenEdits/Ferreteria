package com.xorr.inventario.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xorr.IntegracionBase;
import com.xorr.seguridad.ClienteHttp;

// Asociaciones proveedor-producto con la aplicación real: CRUD, RN-05, RN-07 y RN-13 (sin duplicados).
class ProveedorProductoControllerIntegracionTest extends IntegracionBase {

    private String token;
    private long idProducto;
    private long idProveedor;

    @BeforeEach
    void prepararProductoYProveedor() throws Exception {
        token = registrarEIniciarSesion("Ferretería Asociaciones");
        idProducto = crearProducto(token, 500, 0, 1);
        idProveedor = crearProveedor(token);
    }

    @Test
    void crear_conDatosValidos_responde201ConElPrecioDeCompra() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(idProveedor, idProducto, 300));

        assertEquals(201, respuesta.statusCode());
        assertEquals(300, ClienteHttp.numero(respuesta, "$.precioCompra"));
    }

    @Test
    void crear_repetidaElMismoParProveedorProducto_responde400() throws Exception {
        asociar(token, idProveedor, idProducto, 300);

        assertEquals(400, http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(idProveedor, idProducto, 350)).statusCode());
    }

    @Test
    void crear_conProveedorInexistente_responde404() throws Exception {
        assertEquals(404, http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(ID_INEXISTENTE, idProducto, 300)).statusCode());
    }

    @Test
    void crear_conProductoInexistente_responde404() throws Exception {
        assertEquals(404, http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(idProveedor, ID_INEXISTENTE, 300)).statusCode());
    }

    @Test
    void crear_conPrecioNegativo_responde400() throws Exception {
        assertEquals(400, http.post("/api/proveedor-productos", token,
                cuerpoAsociacion(idProveedor, idProducto, -1)).statusCode());
    }

    @Test
    void crear_siendoEmpleado_responde403() throws Exception {
        String tokenEmpleado = crearEmpleadoEIniciarSesion(token);

        assertEquals(403, http.post("/api/proveedor-productos", tokenEmpleado,
                cuerpoAsociacion(idProveedor, idProducto, 300)).statusCode());
    }

    @Test
    void listar_incluyeLaAsociacionCreada() throws Exception {
        long id = asociar(token, idProveedor, idProducto, 300);

        HttpResponse<String> lista = http.get("/api/proveedor-productos", token);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("\"id\":" + id + ","));
    }

    @Test
    void obtener_asociacionPropia_responde200() throws Exception {
        long id = asociar(token, idProveedor, idProducto, 300);

        assertEquals(200, http.get("/api/proveedor-productos/" + id, token).statusCode());
    }

    @Test
    void obtener_asociacionInexistente_responde404() throws Exception {
        assertEquals(404, http.get("/api/proveedor-productos/" + ID_INEXISTENTE, token).statusCode());
    }

    @Test
    void actualizar_soloElPrecio_responde200YNoLoTomaPorDuplicado() throws Exception {
        long id = asociar(token, idProveedor, idProducto, 300);

        HttpResponse<String> respuesta = http.put("/api/proveedor-productos/" + id, token,
                cuerpoAsociacion(idProveedor, idProducto, 450));

        assertEquals(200, respuesta.statusCode());
        assertEquals(450, ClienteHttp.numero(respuesta, "$.precioCompra"));
    }

    @Test
    void actualizar_haciaUnParQueYaExiste_responde400() throws Exception {
        long otroProducto = crearProducto(token, 700, 0, 1);
        asociar(token, idProveedor, idProducto, 300);
        long idOtra = asociar(token, idProveedor, otroProducto, 400);

        assertEquals(400, http.put("/api/proveedor-productos/" + idOtra, token,
                cuerpoAsociacion(idProveedor, idProducto, 400)).statusCode());
    }

    @Test
    void eliminar_responde204YDespuesNoExiste() throws Exception {
        long id = asociar(token, idProveedor, idProducto, 300);

        assertEquals(204, http.delete("/api/proveedor-productos/" + id, token).statusCode());
        assertEquals(404, http.get("/api/proveedor-productos/" + id, token).statusCode());
    }
}