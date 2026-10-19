package com.xorr.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// RN-05 con la aplicación real: dos empresas distintas, cada una con su administrador
// y su token. Lo que crea una, la otra no lo ve ni lo toca (se trata como "no encontrado").
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AislamientoEmpresasIntegracionTest {

    @LocalServerPort
    private int puerto;

    private ClienteHttp http;
    private String tokenEmpresaA;
    private String tokenEmpresaB;
    private String nombreProductoDeA;
    private long idProductoDeA;

    @BeforeEach
    void crearDosEmpresasYUnProductoDeA() throws Exception {
        http = new ClienteHttp(puerto);
        tokenEmpresaA = registrarEIniciarSesion("Ferretería A");
        tokenEmpresaB = registrarEIniciarSesion("Ferretería B");
        nombreProductoDeA = "Martillo-" + UUID.randomUUID();

        HttpResponse<String> creado = http.post("/api/productos", tokenEmpresaA,
                "{\"nombre\":\"" + nombreProductoDeA + "\",\"precioVenta\":1000,\"stock\":5,\"stockMinimo\":1}");
        assertEquals(201, creado.statusCode());
        idProductoDeA = ClienteHttp.numero(creado, "$.id");
    }

    private String registrarEIniciarSesion(String nombreEmpresa) throws Exception {
        String correo = "admin-" + UUID.randomUUID() + "@correo.com";
        String registro = "{\"nombreEmpresa\":\"" + nombreEmpresa + "\",\"nombre\":\"Admin\",\"correo\":\""
                + correo + "\",\"password\":\"clave12345\"}";
        assertEquals(201, http.post("/api/auth/registro-admin", null, registro).statusCode());

        HttpResponse<String> login = http.post("/api/auth/login", null,
                "{\"correo\":\"" + correo + "\",\"password\":\"clave12345\"}");
        assertEquals(200, login.statusCode());
        return ClienteHttp.texto(login, "$.token");
    }

    @Test
    void laPropiaEmpresa_veSuProducto() throws Exception {
        assertEquals(200, http.get("/api/productos/" + idProductoDeA, tokenEmpresaA).statusCode());
    }

    @Test
    void otraEmpresa_noPuedeVerElProducto_responde404() throws Exception {
        assertEquals(404, http.get("/api/productos/" + idProductoDeA, tokenEmpresaB).statusCode());
    }

    @Test
    void otraEmpresa_noLoVeEnSuListado() throws Exception {
        HttpResponse<String> listaDeB = http.get("/api/productos", tokenEmpresaB);
        HttpResponse<String> listaDeA = http.get("/api/productos", tokenEmpresaA);

        assertFalse(listaDeB.body().contains(nombreProductoDeA));
        assertTrue(listaDeA.body().contains(nombreProductoDeA));
    }

    @Test
    void otraEmpresa_noPuedeEliminarElProducto_responde404() throws Exception {
        assertEquals(404, http.delete("/api/productos/" + idProductoDeA, tokenEmpresaB).statusCode());
        assertEquals(200, http.get("/api/productos/" + idProductoDeA, tokenEmpresaA).statusCode());
    }

    @Test
    void otraEmpresa_noPuedeVenderElProducto_responde404() throws Exception {
        String venta = "{\"detalles\":[{\"idProducto\":" + idProductoDeA + ",\"cantidad\":1}]}";

        assertEquals(404, http.post("/api/facturas", tokenEmpresaB, venta).statusCode());
    }
}