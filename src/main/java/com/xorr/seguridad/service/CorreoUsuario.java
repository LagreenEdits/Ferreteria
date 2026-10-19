package com.xorr.seguridad.service;

import java.util.Locale;

import com.xorr.common.OperacionInvalidaException;
import com.xorr.seguridad.repository.UsuarioRepository;

/**
 * Regla única del correo: "Ana@x.com" y "ana@x.com" son el mismo correo y no
 * puede haber dos usuarios con el mismo (en ninguna empresa). La comparten el
 * registro del administrador y el alta de empleados.
 */
final class CorreoUsuario {

    private CorreoUsuario() {
    }

    static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    static void validarLibre(UsuarioRepository usuarios, String correo) {
        if (usuarios.existsByCorreo(correo)) {
            throw new OperacionInvalidaException("Ya existe un usuario con el correo " + correo);
        }
    }
}
