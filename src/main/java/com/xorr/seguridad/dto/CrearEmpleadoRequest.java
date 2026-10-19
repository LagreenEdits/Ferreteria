package com.xorr.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No lleva rol ni empresa: el rol siempre es EMPLEADO y la empresa sale del token.
public record CrearEmpleadoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
        // Máximo 72: BCrypt solo usa los primeros 72 bytes de la contraseña.
        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {
}