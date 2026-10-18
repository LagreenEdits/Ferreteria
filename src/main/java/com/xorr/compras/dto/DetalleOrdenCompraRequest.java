package com.xorr.compras.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleOrdenCompraRequest(
        @NotNull(message = "El producto es obligatorio") Long idProducto,

        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor que cero") Integer cantidad) {
}