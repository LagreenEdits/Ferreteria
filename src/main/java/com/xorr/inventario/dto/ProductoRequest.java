package com.xorr.inventario.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,

        @NotNull(message = "El precio de venta es obligatorio") @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo") BigDecimal precioVenta,

        @NotNull(message = "El stock es obligatorio") @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,

        @NotNull(message = "El stock mínimo es obligatorio") @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo) {
}