package com.xorr.seguridad.service;

import com.xorr.seguridad.entity.Rol;

// Lo que viaja dentro del JWT: {idUsuario, idEmpresa, rol} (sección 6).
public record DatosToken(Long idUsuario, Long idEmpresa, Rol rol) {
}