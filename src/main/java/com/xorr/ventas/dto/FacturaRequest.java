package com.xorr.ventas.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record FacturaRequest(
        @NotEmpty @Valid List<DetalleFacturaRequest> detalles) {
}