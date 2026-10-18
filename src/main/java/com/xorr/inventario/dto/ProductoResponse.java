package com.xorr.inventario.dto;

import java.math.BigDecimal;

import com.xorr.inventario.entity.Producto;

public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precioVenta,
        int stock,
        int stockMinimo) {

    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecioVenta(),
                producto.getStock(),
                producto.getStockMinimo());
    }
}