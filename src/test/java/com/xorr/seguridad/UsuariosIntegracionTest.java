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

// 1e: alta de empleados y control de roles con la aplicación real.
// Dos empresas, cada una con su administrador; el empleado se crea por la API y se prueba con su token.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsuariosIntegracionTest {

    private static final String MENSAJE_401 = "Debes iniciar sesión con un token válido";
    private static final String MENSAJE_403 = "No tienes permiso para esta operación";
    private static final String CLAVE = "clave12345";

    @LocalServerPort
    private int puerto;

    private ClienteHttp http;
    private String tokenAdminA;
    private String tokenAdminB;
    private String correoEmpleadoDeA;
    private String tokenEmpleadoDeA;

    @BeforeEach
    void crearEmpresasYUnEmpleado() throws Exception {
        http = new ClienteHttp(puerto);
        tokenAdminA = registrarAdminEIniciarSesion("Ferretería A");
        tokenAdminB = registrarAdminEIniciarSesion("Ferretería B");

        correoEmpleadoDeA = "empleado-" + UUID.randomUUID() + "@correo.com";
        HttpResponse<String> creado = http.post("/api/usuarios", tokenAdminA, cuerpoEmpleado(correoEmpleadoDeA, CLAVE));
        assertEquals(201, creado.statusCode());

        tokenEmpleadoDeA = iniciarSesion(correoEmpleadoDeA);
    }

    private String registrarAdminEIniciarSesion(String nombreEmpresa) throws Exception {
        String correo = "admin-" + UUID.randomUUID() + "@correo.com";
        String registro = "{\"nombreEmpresa\":\"" + nombreEmpresa + "\",\"nombre\":\"Admin\",\"correo\":\""
                + correo + "\",\"password\":\"" + CLAVE + "\"}";
        assertEquals(201, http.post("/api/auth/registro-admin", null, registro).statusCode());
        return iniciarSesion(correo);
    }

    private String iniciarSesion(String correo) throws Exception {
        HttpResponse<String> login = http.post("/api/auth/login", null,
                "{\"correo\":\"" + correo + "\",\"password\":\"" + CLAVE + "\"}");
        assertEquals(200, login.statusCode());
        return ClienteHttp.texto(login, "$.token");
    }

    private static String cuerpoEmpleado(String correo, String password) {
        return "{\"nombre\":\"Empleado\",\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    // ---------- 401 ----------

    @Test
    void sinToken_listarUsuarios_responde401() throws Exception {
        HttpResponse<String> respuesta = http.get("/api/usuarios", null);

        assertEquals(401, respuesta.statusCode());
        assertEquals(MENSAJE_401, ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    @Test
    void sinToken_crearUsuario_responde401() throws Exception {
        assertEquals(401, http.post("/api/usuarios", null, cuerpoEmpleado("x@correo.com", CLAVE)).statusCode());
    }

    // ---------- 403: el empleado no administra usuarios ----------

    @Test
    void empleado_noPuedeListarUsuarios_responde403() throws Exception {
        HttpResponse<String> respuesta = http.get("/api/usuarios", tokenEmpleadoDeA);

        assertEquals(403, respuesta.statusCode());
        assertEquals(MENSAJE_403, ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    @Test
    void empleado_noPuedeCrearUsuarios_responde403() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/usuarios", tokenEmpleadoDeA,
                cuerpoEmpleado("otro-" + UUID.randomUUID() + "@correo.com", CLAVE));

        assertEquals(403, respuesta.statusCode());
        assertEquals(MENSAJE_403, ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    // ---------- ADMIN ----------

    @Test
    void admin_listaUsuariosDeSuEmpresa_incluyendoAlEmpleado() throws Exception {
        HttpResponse<String> lista = http.get("/api/usuarios", tokenAdminA);

        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains(correoEmpleadoDeA));
    }

    @Test
    void empleadoCreado_puedeIniciarSesionYUsarVentas() throws Exception {
        assertEquals(200, http.get("/api/productos", tokenEmpleadoDeA).statusCode());
        assertEquals(200, http.get("/api/facturas", tokenEmpleadoDeA).statusCode());
    }

    @Test
    void admin_crearEmpleadoConContrasenaCorta_responde400() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/usuarios", tokenAdminA,
                cuerpoEmpleado("corta-" + UUID.randomUUID() + "@correo.com", "123"));

        assertEquals(400, respuesta.statusCode());
    }

    @Test
    void admin_crearEmpleadoSinCuerpoValido_responde400() throws Exception {
        assertEquals(400, http.post("/api/usuarios", tokenAdminA, "{}").statusCode());
    }

    // ---------- Aislamiento entre empresas ----------

    @Test
    void otraEmpresa_noVeLosUsuariosDeA() throws Exception {
        HttpResponse<String> listaDeB = http.get("/api/usuarios", tokenAdminB);

        assertEquals(200, listaDeB.statusCode());
        assertFalse(listaDeB.body().contains(correoEmpleadoDeA));
    }

    @Test
    void empleadoCreadoPorB_noApareceEnLaListaDeA() throws Exception {
        String correoDeB = "empleado-b-" + UUID.randomUUID() + "@correo.com";
        assertEquals(201, http.post("/api/usuarios", tokenAdminB, cuerpoEmpleado(correoDeB, CLAVE)).statusCode());

        HttpResponse<String> listaDeA = http.get("/api/usuarios", tokenAdminA);

        assertFalse(listaDeA.body().contains(correoDeB));
    }
}