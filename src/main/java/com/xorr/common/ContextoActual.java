package com.xorr.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.xorr.seguridad.service.DatosToken;

/**
 * Única fuente del idEmpresa y del idUsuario: los lee del token JWT de la
 * petición actual (RN-05). Ningún servicio los recibe desde fuera.
 * Dependencia common → seguridad justificada: esta es la única clase que lee
 * el token; se revisa en el refactor de la Fase 2.
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