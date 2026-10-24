package com.xorr.compras.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.compras.dto.OrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraResponse;
import com.xorr.compras.entity.EstadoOrdenCompra;
import com.xorr.compras.entity.OrdenCompra;
import com.xorr.compras.repository.OrdenCompraRepository;
import com.xorr.inventario.service.ProductoService;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenes;
    private final ContextoActual contexto;
    private final ProductoService productoService;
    private final ArmadorDeOrden armador;

    public OrdenCompraService(OrdenCompraRepository ordenes,
            ContextoActual contexto,
            ProductoService productoService,
            ArmadorDeOrden armador) {
        this.ordenes = ordenes;
        this.contexto = contexto;
        this.productoService = productoService;
        this.armador = armador;
    }

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest datos) {
        return OrdenCompraResponse.desde(ordenes.save(armador.armar(datos)));
    }

    @Transactional(readOnly = true)
    public List<OrdenCompraResponse> listar() {
        return ordenes.findByIdEmpresa(contexto.getIdEmpresa()).stream()
                .map(OrdenCompraResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse obtener(Long idOrden) {
        return OrdenCompraResponse.desde(buscarOrden(idOrden));
    }

    // RN-02 y RN-06: si falla el aumento de stock de una línea, la transacción se
    // revierte completa.
    @Transactional
    public OrdenCompraResponse recibir(Long idOrden) {
        OrdenCompra orden = buscarOrden(idOrden);
        validarPendiente(orden, "recibir");
        orden.getDetalles().forEach(
                detalle -> productoService.incrementarStock(detalle.getIdProducto(), detalle.getCantidad()));
        orden.setEstado(EstadoOrdenCompra.RECIBIDA);
        return OrdenCompraResponse.desde(ordenes.save(orden));
    }

    @Transactional
    public OrdenCompraResponse cancelar(Long idOrden) {
        OrdenCompra orden = buscarOrden(idOrden);
        validarPendiente(orden, "cancelar");
        orden.setEstado(EstadoOrdenCompra.CANCELADA);
        return OrdenCompraResponse.desde(ordenes.save(orden));
    }

    // RN-08: una orden solo se recibe o se cancela mientras está PENDIENTE.
    private void validarPendiente(OrdenCompra orden, String accion) {
        if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE) {
            throw new OperacionInvalidaException(
                    "No se puede " + accion + " una orden en estado " + orden.getEstado());
        }
    }

    private OrdenCompra buscarOrden(Long idOrden) {
        return ordenes.findByIdAndIdEmpresa(idOrden, contexto.getIdEmpresa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada: " + idOrden));
    }
}