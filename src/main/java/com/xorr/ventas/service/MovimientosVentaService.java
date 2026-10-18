package com.xorr.ventas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.ventas.repository.FacturaRepository;

/**
 * Consulta de solo lectura para que Inventario aplique RN-12 sin tocar el
 * repositorio de Ventas. Solo depende de su repositorio (no de
 * ProductoService), así que no crea un ciclo entre beans.
 */
@Service
public class MovimientosVentaService {

    private final FacturaRepository facturas;
    private final ContextoActual contexto;

    public MovimientosVentaService(FacturaRepository facturas, ContextoActual contexto) {
        this.facturas = facturas;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public boolean productoTieneFacturas(Long idProducto) {
        return facturas.existeConProducto(contexto.getIdEmpresa(), idProducto);
    }
}