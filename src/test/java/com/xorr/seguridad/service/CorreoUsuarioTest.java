package com.xorr.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.xorr.common.OperacionInvalidaException;
import com.xorr.seguridad.repository.UsuarioRepository;

class CorreoUsuarioTest {

    @Test
    void normalizar_quitaEspaciosYPasaAMinusculas() {
        assertEquals("ana@x.com", CorreoUsuario.normalizar("  Ana@X.com "));
    }

    @Test
    void validarLibre_correoRepetido_lanzaErrorConElCorreo() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(usuarios.existsByCorreo("ana@x.com")).thenReturn(true);

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> CorreoUsuario.validarLibre(usuarios, "ana@x.com"));

        assertEquals("Ya existe un usuario con el correo ana@x.com", error.getMessage());
    }

    @Test
    void validarLibre_correoLibre_noLanzaNada() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(usuarios.existsByCorreo("luis@x.com")).thenReturn(false);

        assertDoesNotThrow(() -> CorreoUsuario.validarLibre(usuarios, "luis@x.com"));
    }
}
