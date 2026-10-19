package com.xorr.common;

// Lo que viaja dentro del JWT: {idUsuario, idEmpresa, rol} (sección 6).
public record DatosToken(Long idUsuario, Long idEmpresa, Rol rol) {
}