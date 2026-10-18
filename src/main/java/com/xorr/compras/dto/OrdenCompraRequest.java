package com.xorr.compras.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrdenCompraRequest(
        @NotNull(message = "El proveedor es obligatorio") Long idProveedor,

        @NotEmpty(message = "La orden debe tener al menos un producto") @Valid List<DetalleOrdenCompraRequest> detalles) {
}