package com.xorr.seguridad.dto;

import com.xorr.common.Rol;
import com.xorr.seguridad.entity.Usuario;

// Nunca incluye passwordHash ni idEmpresa.
public record UsuarioResponse(Long id, String nombre, String correo, Rol rol) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}