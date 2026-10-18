package com.xorr.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.xorr.ventas.entity.Factura;

public record FacturaResponse(
        Long id,
        Long idUsuario,
        LocalDateTime fecha,
        BigDecimal total,
        List<DetalleFacturaResponse> detalles) {

    public static FacturaResponse desde(Factura factura) {
        return new FacturaResponse(
                factura.getId(),
                factura.getIdUsuario(),
                factura.getFecha(),
                factura.getTotal(),
                factura.getDetalles().stream().map(DetalleFacturaResponse::desde).toList());
    }
}