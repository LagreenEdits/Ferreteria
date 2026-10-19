package com.xorr.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Única fuente del idEmpresa y del idUsuario: los lee del token JWT de la
 * petición actual (RN-05). Ningún servicio los recibe desde fuera.
 * DatosToken vive en common, así que common ya no depende de seguridad.
 */
@Component
public class ContextoActual {

    public Long getIdEmpresa() {
        return datos().idEmpresa();
    }

    public Long getIdUsuario() {
        return datos().idUsuario();
    }

    // Si llega aquí sin sesión es un error de programación (el filtro de seguridad
    // ya rechaza las peticiones sin token), no un error del usuario.
    private static DatosToken datos() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof DatosToken datos) {
            return datos;
        }
        throw new IllegalStateException("No hay una sesión autenticada");
    }
}