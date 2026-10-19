package com.xorr.seguridad.dto;

// El token se envía luego en la cabecera "Authorization: Bearer <token>".
public record LoginResponse(String token, UsuarioResponse usuario) {
}