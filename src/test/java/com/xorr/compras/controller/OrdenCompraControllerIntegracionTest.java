package com.xorr.compras.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xorr.IntegracionBase;
import com.xorr.seguridad.ClienteHttp;

// Órdenes de compra con la aplicación real: ciclo completo y reglas RN-02, RN-03, RN-05, RN-08, RN-09 y RN-16.
class OrdenCompraControllerIntegracionTest extends IntegracionBase {

    private String token;
    private long idProducto;
    private long idProveedor;

    @BeforeEach
    void prepararProductoProveedorYAsociacion() throws Exception {
        token = registrarEIniciarSesion("Ferretería Compras");
        idProducto = crearProducto(token, 500, 0, 1);
        idProveedor = crearProveedor(token);
        asociar(token, idProveedor, idProducto, 300);
    }

    @Test
    void crear_conDatosValidos_responde201YCalculaElTotal() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/ordenes-compra", token,
                ordenConUnaLinea(idProveedor, idProducto, 2));

        assertEquals(201, respuesta.statusCode());
        assertEquals("PENDIENTE", ClienteHttp.texto(respuesta, "$.estado"));
        assertEquals(600, ClienteHttp.numero(respuesta, "$.total"));
    }

    @Test
    void crear_conElMismoProductoEnDosLineas_responde400() throws Exception {
        String repetido = "{\"idProveedor\":" + idProveedor + ",\"detalles\":["
                + "{\"idProducto\":" + idProducto + ",\"cantidad\":1},"
                + "{\"idProducto\":" + idProducto + ",\"cantidad\":2}]}";

        assertEquals(400, http.post("/api/ordenes-compra", token, repetido).statusCode());
    }

    @Test
    void crear_conProductoNoAsociadoAlProveedor_responde400() throws Exception {
        long productoSinAsociar = crearProducto(token, 800, 0, 1);

        HttpResponse<String> respuesta = http.post("/api/ordenes-compra", token,
                ordenConUnaLinea(idProveedor, productoSinAsociar, 1));

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void crear_sinToken_responde401() throws Exception {
        assertEquals(401, http.post("/api/ordenes-compra", null,
                ordenConUnaLinea(idProveedor, idProducto, 1)).statusCode());
    }

    @Test
    void crear_siendoEmpleado_responde403() throws Exception {
        String tokenEmpleado = crearEmpleadoEIniciarSesion(token);

        assertEquals(403, http.post("/api/ordenes-compra", tokenEmpleado,
                ordenConUnaLinea(idProveedor, idProducto, 1)).statusCode());
    }

    @Test
    void listar_incluyeLaOrdenCreada() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 1);

        HttpResponse<String> lista = http.get("/api/ordenes-compra", token);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("\"id\":" + idOrden + ","));
    }

    @Test
    void obtener_ordenPropia_responde200() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 4);

        HttpResponse<String> respuesta = http.get("/api/ordenes-compra/" + idOrden, token);

        assertEquals(200, respuesta.statusCode());
        assertEquals(idOrden, ClienteHttp.numero(respuesta, "$.id"));
        assertEquals(1200, ClienteHttp.numero(respuesta, "$.total"));
    }

    @Test
    void obtener_ordenDeOtraEmpresa_responde404() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 1);
        String tokenOtraEmpresa = registrarEIniciarSesion("Ferretería Ajena");

        assertEquals(404, http.get("/api/ordenes-compra/" + idOrden, tokenOtraEmpresa).statusCode());
    }

    @Test
    void recibir_ordenPendiente_incrementaElStockYCambiaElEstado() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 5);

        HttpResponse<String> recibida = http.put("/api/ordenes-compra/" + idOrden + "/recibir", token, null);

        assertEquals(200, recibida.statusCode());
        assertEquals("RECIBIDA", ClienteHttp.texto(recibida, "$.estado"));
        assertEquals(5, stockDe(token, idProducto));
    }

    @Test
    void recibir_dosVeces_laSegundaResponde400YNoSumaDeNuevo() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 3);
        http.put("/api/ordenes-compra/" + idOrden + "/recibir", token, null);

        assertEquals(400, http.put("/api/ordenes-compra/" + idOrden + "/recibir", token, null).statusCode());
        assertEquals(3, stockDe(token, idProducto));
    }

    @Test
    void cancelar_ordenPendiente_responde200YNoTocaElStock() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 2);

        HttpResponse<String> cancelada = http.put("/api/ordenes-compra/" + idOrden + "/cancelar", token, null);

        assertEquals(200, cancelada.statusCode());
        assertEquals("CANCELADA", ClienteHttp.texto(cancelada, "$.estado"));
        assertEquals(0, stockDe(token, idProducto));
    }

    @Test
    void recibir_ordenCancelada_responde400() throws Exception {
        long idOrden = crearOrden(token, idProveedor, idProducto, 2);
        http.put("/api/ordenes-compra/" + idOrden + "/cancelar", token, null);

        assertEquals(400, http.put("/api/ordenes-compra/" + idOrden + "/recibir", token, null).statusCode());
    }
}