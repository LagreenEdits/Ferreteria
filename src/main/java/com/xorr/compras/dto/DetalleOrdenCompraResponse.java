package com.xorr.compras.dto;

import java.math.BigDecimal;

import com.xorr.compras.entity.DetalleOrdenCompra;

public record DetalleOrdenCompraResponse(
        Long idProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public static DetalleOrdenCompraResponse desde(DetalleOrdenCompra detalle) {
        return new DetalleOrdenCompraResponse(
                detalle.getIdProducto(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }
}