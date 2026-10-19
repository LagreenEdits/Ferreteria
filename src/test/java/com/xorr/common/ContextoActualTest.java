package com.xorr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.xorr.common.DatosToken;
import com.xorr.common.Rol;

class ContextoActualTest {

    private final ContextoActual contexto = new ContextoActual();

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conSesion_devuelveLaEmpresaYElUsuarioDelToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new DatosToken(3L, 7L, Rol.EMPLEADO), null, List.of()));

        assertEquals(7L, contexto.getIdEmpresa());
        assertEquals(3L, contexto.getIdUsuario());
    }

    @Test
    void sinSesion_lanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, contexto::getIdEmpresa);
        assertThrows(IllegalStateException.class, contexto::getIdUsuario);
    }

    @Test
    void conUnPrincipalQueNoEsDelToken_lanzaIllegalStateException() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThrows(IllegalStateException.class, contexto::getIdEmpresa);
    }
}