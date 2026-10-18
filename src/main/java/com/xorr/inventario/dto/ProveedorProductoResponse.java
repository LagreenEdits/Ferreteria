package com.xorr.inventario.dto;

import java.math.BigDecimal;

import com.xorr.inventario.entity.ProveedorProducto;

public record ProveedorProductoResponse(
        Long id,
        Long idProveedor,
        Long idProducto,
        BigDecimal precioCompra) {

    public static ProveedorProductoResponse desde(ProveedorProducto asociacion) {
        return new ProveedorProductoResponse(
                asociacion.getId(),
                asociacion.getIdProveedor(),
                asociacion.getIdProducto(),
                asociacion.getPrecioCompra());
    }
}