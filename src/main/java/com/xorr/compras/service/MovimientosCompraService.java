package com.xorr.compras.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.compras.repository.OrdenCompraRepository;

/**
 * Consulta de solo lectura para que Inventario aplique RN-12 sin tocar el
 * repositorio de Compras. Solo depende de su repositorio (no de
 * ProductoService), así que no crea un ciclo entre beans.
 */
@Service
public class MovimientosCompraService {

    private final OrdenCompraRepository ordenes;
    private final ContextoActual contexto;

    public MovimientosCompraService(OrdenCompraRepository ordenes, ContextoActual contexto) {
        this.ordenes = ordenes;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public boolean productoTieneOrdenes(Long idProducto) {
        return ordenes.existeConProducto(contexto.getIdEmpresa(), idProducto);
    }

    @Transactional(readOnly = true)
    public boolean proveedorTieneOrdenes(Long idProveedor) {
        return ordenes.existsByIdEmpresaAndIdProveedor(contexto.getIdEmpresa(), idProveedor);
    }
}