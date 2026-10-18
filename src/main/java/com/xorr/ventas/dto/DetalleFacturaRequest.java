package com.xorr.ventas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// RN-07: Integer (no int) para que una cantidad omitida no se vuelva 0 sin avisar.
public record DetalleFacturaRequest(
        @NotNull Long idProducto,
        @NotNull @Min(1) Integer cantidad) {
}