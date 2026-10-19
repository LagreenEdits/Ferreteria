package com.xorr.inventario.service;

/**
 * Lo que Inventario necesita saber de Ventas para aplicar RN-12. Inventario
 * define la interfaz y Ventas la implementa.
 */
public interface MovimientosVenta {

    boolean productoTieneFacturas(Long idProducto);
}
