package com.xorr.ventas.dto;

import java.math.BigDecimal;

import com.xorr.ventas.entity.DetalleFactura;

public record DetalleFacturaResponse(
        Long idProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public static DetalleFacturaResponse desde(DetalleFactura detalle) {
        return new DetalleFacturaResponse(
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }
}