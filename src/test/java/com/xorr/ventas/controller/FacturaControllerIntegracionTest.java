package com.xorr.ventas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xorr.IntegracionBase;
import com.xorr.seguridad.ClienteHttp;

// Facturas con la aplicación real: RN-01 (409 sin stock), RN-03, RN-05, RN-06, RN-10 y RN-15.
class FacturaControllerIntegracionTest extends IntegracionBase {

    private String token;
    private long idProducto;

    @BeforeEach
    void prepararProductoConStock() throws Exception {
        token = registrarEIniciarSesion("Ferretería Ventas");
        idProducto = crearProducto(token, 1000, 10, 1);
    }

    @Test
    void crear_conStockSuficiente_responde201CalculaElTotalYDescuentaElStock() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 3));

        assertEquals(201, respuesta.statusCode());
        assertEquals(3000, ClienteHttp.numero(respuesta, "$.total"));
        assertEquals(7, stockDe(token, idProducto));
    }

    @Test
    void crear_conStockInsuficiente_responde409YNoDescuentaNada() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 11));

        assertEquals(409, respuesta.statusCode());
        assertEquals(10, stockDe(token, idProducto));
    }

    @Test
    void crear_conElMismoProductoEnDosLineas_responde400YNoDescuentaNada() throws Exception {
        String repetido = "{\"detalles\":[{\"idProducto\":" + idProducto + ",\"cantidad\":1},"
                + "{\"idProducto\":" + idProducto + ",\"cantidad\":2}]}";

        assertEquals(400, http.post("/api/facturas", token, repetido).statusCode());
        assertEquals(10, stockDe(token, idProducto));
    }

    @Test
    void crear_sinLineas_responde400() throws Exception {
        assertEquals(400, http.post("/api/facturas", token, "{\"detalles\":[]}").statusCode());
    }

    @Test
    void crear_conProductoInexistente_responde404() throws Exception {
        assertEquals(404, http.post("/api/facturas", token,
                facturaConUnaLinea(ID_INEXISTENTE, 1)).statusCode());
    }

    @Test
    void crear_sinToken_responde401() throws Exception {
        assertEquals(401, http.post("/api/facturas", null, facturaConUnaLinea(idProducto, 1)).statusCode());
    }

    @Test
    void crear_siendoEmpleado_responde201() throws Exception {
        String tokenEmpleado = crearEmpleadoEIniciarSesion(token);

        assertEquals(201, http.post("/api/facturas", tokenEmpleado, facturaConUnaLinea(idProducto, 1)).statusCode());
    }

    @Test
    void listar_incluyeLaFacturaCreada() throws Exception {
        long idFactura = ClienteHttp.numero(
                http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 1)), "$.id");

        HttpResponse<String> lista = http.get("/api/facturas", token);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("\"id\":" + idFactura + ","));
    }

    @Test
    void obtener_facturaPropia_responde200() throws Exception {
        long idFactura = ClienteHttp.numero(
                http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 2)), "$.id");

        HttpResponse<String> respuesta = http.get("/api/facturas/" + idFactura, token);

        assertEquals(200, respuesta.statusCode());
        assertEquals(2000, ClienteHttp.numero(respuesta, "$.total"));
    }

    @Test
    void obtener_facturaDeOtraEmpresa_responde404() throws Exception {
        long idFactura = ClienteHttp.numero(
                http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 1)), "$.id");
        String tokenOtraEmpresa = registrarEIniciarSesion("Ferretería Ajena");

        assertEquals(404, http.get("/api/facturas/" + idFactura, tokenOtraEmpresa).statusCode());
    }
}