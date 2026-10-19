package com.xorr.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.xorr.common.DatosToken;
import com.xorr.common.Rol;
import com.xorr.seguridad.entity.Usuario;

// Sin Spring ni Mockito: JwtService se crea directamente con su secreto.
class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-con-mas-de-32-caracteres";

    private final JwtService servicio = new JwtService(SECRETO, 60);

    private static Usuario usuario(Long id, Long idEmpresa, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setIdEmpresa(idEmpresa);
        usuario.setRol(rol);
        return usuario;
    }

    @Test
    void generarYLeer_devuelveLosMismosDatos() {
        String token = servicio.generar(usuario(3L, 7L, Rol.EMPLEADO));

        DatosToken datos = servicio.leer(token).orElseThrow();

        assertEquals(3L, datos.idUsuario());
        assertEquals(7L, datos.idEmpresa());
        assertEquals(Rol.EMPLEADO, datos.rol());
    }

    @Test
    void leer_tokenFirmadoConOtraClave_seRechaza() {
        String token = new JwtService(OTRO_SECRETO, 60).generar(usuario(3L, 7L, Rol.ADMIN));

        assertEquals(Optional.empty(), servicio.leer(token));
    }

    @Test
    void leer_tokenVencido_seRechaza() {
        String vencido = new JwtService(SECRETO, -1).generar(usuario(3L, 7L, Rol.ADMIN));

        assertEquals(Optional.empty(), servicio.leer(vencido));
    }

    @Test
    void leer_tokenAlterado_seRechaza() {
        String token = servicio.generar(usuario(3L, 7L, Rol.ADMIN));

        assertEquals(Optional.empty(), servicio.leer(token + "x"));
    }

    @Test
    void leer_textoQueNoEsUnToken_seRechaza() {
        assertEquals(Optional.empty(), servicio.leer("esto-no-es-un-token"));
    }

    @Test
    void leer_tokenNuloOVacio_seRechaza() {
        assertEquals(Optional.empty(), servicio.leer(null));
        assertEquals(Optional.empty(), servicio.leer(""));
    }

    @Test
    void crear_conSecretoCorto_fallaAlArrancarConMensajeClaro() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> new JwtService("corto", 60));

        assertTrue(error.getMessage().contains("xorr.jwt.secret"));
    }

    @Test
    void crear_sinSecreto_falla() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 60));
    }
}