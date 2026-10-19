package com.xorr.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.xorr.common.Rol;
import com.xorr.seguridad.entity.Usuario;
import com.xorr.seguridad.service.JwtService;

// Aplicación completa en un puerto real: comprueba quién puede llamar a qué (sección 6).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadIntegracionTest {

    private static final String MENSAJE_401 = "Debes iniciar sesión con un token válido";
    private static final String MENSAJE_403 = "No tienes permiso para esta operación";

    @LocalServerPort
    private int puerto;
    @Autowired
    private JwtService jwtService;
    @Value("${xorr.jwt.secret}")
    private String secreto;

    private ClienteHttp http;

    @BeforeEach
    void preparar() {
        http = new ClienteHttp(puerto);
    }

    private static String tokenDe(JwtService servicio, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setIdEmpresa(1L);
        usuario.setRol(rol);
        return servicio.generar(usuario);
    }

    private String token(Rol rol) {
        return tokenDe(jwtService, rol);
    }

    // ---------- 401: sin sesión válida ----------

    @Test
    void sinToken_rutaProtegida_responde401ConMensajeJson() throws Exception {
        HttpResponse<String> respuesta = http.get("/api/productos", null);

        assertEquals(401, respuesta.statusCode());
        assertEquals(MENSAJE_401, ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    @Test
    void tokenQueNoEsUnToken_responde401() throws Exception {
        assertEquals(401, http.get("/api/productos", "esto-no-es-un-token").statusCode());
    }

    @Test
    void tokenVencido_responde401() throws Exception {
        String vencido = tokenDe(new JwtService(secreto, -1), Rol.ADMIN);

        assertEquals(401, http.get("/api/productos", vencido).statusCode());
    }

    @Test
    void tokenFirmadoConOtraClave_responde401() throws Exception {
        String ajeno = tokenDe(new JwtService("otro-secreto-distinto-con-mas-de-32-caracteres", 60), Rol.ADMIN);

        assertEquals(401, http.get("/api/productos", ajeno).statusCode());
    }

    // ---------- ADMIN ----------

    @ParameterizedTest
    @ValueSource(strings = { "/api/productos", "/api/productos/stock-bajo", "/api/proveedores",
            "/api/proveedor-productos", "/api/ordenes-compra", "/api/facturas" })
    void admin_puedeConsultarTodo(String ruta) throws Exception {
        assertEquals(200, http.get(ruta, token(Rol.ADMIN)).statusCode());
    }

    // ---------- EMPLEADO: solo ventas y consultas ----------

    @ParameterizedTest
    @ValueSource(strings = { "/api/productos", "/api/productos/stock-bajo", "/api/facturas" })
    void empleado_puedeConsultarProductosYFacturas(String ruta) throws Exception {
        assertEquals(200, http.get(ruta, token(Rol.EMPLEADO)).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/proveedores", "/api/proveedor-productos", "/api/ordenes-compra" })
    void empleado_noPuedeConsultarProveedoresNiCompras_responde403(String ruta) throws Exception {
        HttpResponse<String> respuesta = http.get(ruta, token(Rol.EMPLEADO));

        assertEquals(403, respuesta.statusCode());
        assertEquals(MENSAJE_403, ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    @ParameterizedTest
    @CsvSource({ "POST,/api/productos", "PUT,/api/productos/1", "DELETE,/api/productos/1",
            "POST,/api/ordenes-compra" })
    void empleado_noPuedeEscribirProductosNiComprar_responde403(String metodo, String ruta) throws Exception {
        assertEquals(403, http.llamar(metodo, ruta, token(Rol.EMPLEADO), "{}").statusCode());
    }

    // ---------- Rutas públicas ----------

    @Test
    void login_esPublico_conCredencialesMalasResponde401DelLoginNoDelFiltro() throws Exception {
        HttpResponse<String> respuesta = http.post("/api/auth/login", null,
                "{\"correo\":\"nadie@correo.com\",\"password\":\"clave12345\"}");

        assertEquals(401, respuesta.statusCode());
        assertEquals("Correo o contraseña incorrectos", ClienteHttp.texto(respuesta, "$.mensaje"));
    }

    @Test
    void registroAdmin_esPublico_llegaAlControladorYValidaElCuerpo() throws Exception {
        assertEquals(400, http.post("/api/auth/registro-admin", null, "{}").statusCode());
    }

    @Test
    void documentacionOpenApi_esPublicaYIncluyeElEsquemaDelToken() throws Exception {
        HttpResponse<String> respuesta = http.get("/v3/api-docs", null);

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("bearerAuth"));
    }

    // ---------- Todo lo demás se deniega ----------

    @Test
    void rutaQueNoEstaEnLasReglas_seDeniegaAunqueSeaAdmin() throws Exception {
        assertEquals(403, http.get("/api/no-existe", token(Rol.ADMIN)).statusCode());
    }

    // RN-11: la seguridad no debe tapar el 405 que da Spring (pasa por /error).
    @ParameterizedTest
    @ValueSource(strings = { "PUT", "DELETE" })
    void facturas_noSeModificanNiSeBorran_responde405ConToken(String metodo) throws Exception {
        assertEquals(405, http.llamar(metodo, "/api/facturas/1", token(Rol.ADMIN), "{}").statusCode());
    }
}