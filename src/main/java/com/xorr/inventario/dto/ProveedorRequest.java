package com.xorr.inventario.dto;

import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,

        String telefono) {
}