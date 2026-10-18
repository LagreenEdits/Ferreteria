package com.xorr.inventario.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ProveedorProductoRequest(
        @NotNull(message = "El proveedor es obligatorio") Long idProveedor,

        @NotNull(message = "El producto es obligatorio") Long idProducto,

        @NotNull(message = "El precio de compra es obligatorio") @DecimalMin(value = "0.0", message = "El precio de compra no puede ser negativo") BigDecimal precioCompra) {
}