package com.xorr.inventario.service;

/**
 * Lo que Inventario necesita saber de Compras para aplicar RN-12 (no eliminar
 * con movimientos). Inventario define la interfaz y Compras la implementa: así
 * Inventario ya no depende de Compras.
 */
public interface MovimientosCompra {

    boolean productoTieneOrdenes(Long idProducto);

    boolean proveedorTieneOrdenes(Long idProveedor);
}
