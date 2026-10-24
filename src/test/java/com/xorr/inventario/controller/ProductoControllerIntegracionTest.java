package com.xorr.inventario.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xorr.IntegracionBase;
import com.xorr.seguridad.ClienteHttp;

// Productos con la aplicación real: CRUD, RN-04 (stock bajo), RN-07, RN-12 (no se elimina con movimientos) y permisos por rol.
class ProductoControllerIntegracionTest extends IntegracionBase {

    private String token;

    @BeforeEach
    void iniciarSesionComoAdministrador() throws Exception {
        token = registrarEIniciarSesion("Ferretería Productos");
    }

    @Test
    void crear_sinNombre_responde400() throws Exception {
        assertEquals(400, http.post("/api/productos", token, cuerpoProducto("", 500, 1, 1)).statusCode());
    }

    @Test
    void crear_conStockNegativo_responde400() throws Exception {
        assertEquals(400, http.post("/api/productos", token,
                cuerpoProducto("Clavo", 500, -1, 1)).statusCode());
    }

    @Test
    void listar_incluyeElProductoCreado() throws Exception {
        long id = crearProducto(token, 500, 1, 1);

        HttpResponse<String> lista = http.get("/api/productos", token);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("\"id\":" + id + ","));
    }

    @Test
    void obtener_productoInexistente_responde404() throws Exception {
        assertEquals(404, http.get("/api/productos/" + ID_INEXISTENTE, token).statusCode());
    }

    @Test
    void stockBajo_soloListaLosQueTienenMenosDelMinimo() throws Exception {
        String bajo = "Bajo-" + UUID.randomUUID();
        String suficiente = "Suficiente-" + UUID.randomUUID();
        crearProductoConNombre(token, bajo, 500, 0, 5);
        crearProductoConNombre(token, suficiente, 500, 50, 1);

        HttpResponse<String> respuesta = http.get("/api/productos/stock-bajo", token);

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains(bajo));
        assertFalse(respuesta.body().contains(suficiente));
    }

    @Test
    void stockBajo_conStockIgualAlMinimo_noLoIncluye() throws Exception {
        String igual = "Igual-" + UUID.randomUUID();
        crearProductoConNombre(token, igual, 500, 5, 5);

        assertFalse(http.get("/api/productos/stock-bajo", token).body().contains(igual));
    }

    @Test
    void actualizar_cambiaPrecioYStock() throws Exception {
        long id = crearProducto(token, 500, 1, 1);

        HttpResponse<String> respuesta = http.put("/api/productos/" + id, token,
                cuerpoProducto("Producto editado", 900, 20, 2));

        assertEquals(200, respuesta.statusCode());
        assertEquals(900, ClienteHttp.numero(respuesta, "$.precioVenta"));
        assertEquals(20, stockDe(token, id));
    }

    @Test
    void actualizar_productoInexistente_responde404() throws Exception {
        assertEquals(404, http.put("/api/productos/" + ID_INEXISTENTE, token,
                cuerpoProducto("Fantasma", 900, 20, 2)).statusCode());
    }

    @Test
    void eliminar_sinMovimientos_responde204YDespuesNoExiste() throws Exception {
        long id = crearProducto(token, 500, 1, 1);

        assertEquals(204, http.delete("/api/productos/" + id, token).statusCode());
        assertEquals(404, http.get("/api/productos/" + id, token).statusCode());
    }

    @Test
    void eliminar_conOrdenesDeCompra_responde400() throws Exception {
        long idProducto = crearProducto(token, 500, 0, 1);
        long idProveedor = crearProveedor(token);
        asociar(token, idProveedor, idProducto, 300);
        crearOrden(token, idProveedor, idProducto, 1);

        assertEquals(400, http.delete("/api/productos/" + idProducto, token).statusCode());
    }

    @Test
    void eliminar_conFacturas_responde400() throws Exception {
        long idProducto = crearProducto(token, 500, 5, 1);
        assertEquals(201, http.post("/api/facturas", token, facturaConUnaLinea(idProducto, 1)).statusCode());

        assertEquals(400, http.delete("/api/productos/" + idProducto, token).statusCode());
    }

    @Test
    void eliminar_borraEnCascadaSusAsociaciones() throws Exception {
        long idProducto = crearProducto(token, 500, 0, 1);
        long idProveedor = crearProveedor(token);
        long idAsociacion = asociar(token, idProveedor, idProducto, 300);

        assertEquals(204, http.delete("/api/productos/" + idProducto, token).statusCode());
        assertEquals(404, http.get("/api/proveedor-productos/" + idAsociacion, token).statusCode());
    }

    @Test
    void empleado_puedeLeerPeroNoEscribir() throws Exception {
        long id = crearProducto(token, 500, 1, 1);
        String tokenEmpleado = crearEmpleadoEIniciarSesion(token);

        assertEquals(200, http.get("/api/productos/" + id, tokenEmpleado).statusCode());
        assertEquals(403, http.post("/api/productos", tokenEmpleado,
                cuerpoProducto("No", 500, 1, 1)).statusCode());
        assertEquals(403, http.delete("/api/productos/" + id, tokenEmpleado).statusCode());
    }
}