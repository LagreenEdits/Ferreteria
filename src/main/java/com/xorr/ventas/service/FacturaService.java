package com.xorr.ventas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProductoResponse;
import com.xorr.inventario.service.ProductoService;
import com.xorr.ventas.dto.DetalleFacturaRequest;
import com.xorr.ventas.dto.FacturaRequest;
import com.xorr.ventas.dto.FacturaResponse;
import com.xorr.ventas.entity.DetalleFactura;
import com.xorr.ventas.entity.Factura;
import com.xorr.ventas.repository.FacturaRepository;

// RN-11: no hay anulación ni borrado de facturas, por eso este servicio solo
// tiene crear, listar y obtener.
@Service
public class FacturaService {

    private final FacturaRepository facturas;
    private final ContextoActual contexto;
    private final ProductoService productoService;

    public FacturaService(FacturaRepository facturas,
            ContextoActual contexto,
            ProductoService productoService) {
        this.facturas = facturas;
        this.contexto = contexto;
        this.productoService = productoService;
    }

    // RN-06: si falla el descuento de stock de una línea (RN-01), la transacción
    // se revierte completa y no se guarda nada.
    @Transactional
    public FacturaResponse crear(FacturaRequest datos) {
        validarSinProductosRepetidos(datos.detalles());
        Factura factura = nuevaFactura();
        datos.detalles().forEach(linea -> factura.getDetalles().add(armarDetalle(linea)));
        factura.setTotal(sumarSubtotales(factura));
        return FacturaResponse.desde(facturas.save(factura));
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar() {
        return facturas.findByIdEmpresa(contexto.getIdEmpresa()).stream()
                .map(FacturaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtener(Long idFactura) {
        return FacturaResponse.desde(buscarFactura(idFactura));
    }

    // RN-15: un producto solo puede aparecer en una línea de la factura.
    private void validarSinProductosRepetidos(List<DetalleFacturaRequest> lineas) {
        long distintos = lineas.stream().map(DetalleFacturaRequest::idProducto).distinct().count();
        if (distintos != lineas.size()) {
            throw new OperacionInvalidaException(
                    "La factura repite un producto en varias líneas; usa una sola línea con la cantidad total");
        }
    }

    private Factura nuevaFactura() {
        Factura factura = new Factura();
        factura.setIdEmpresa(contexto.getIdEmpresa());
        factura.setIdUsuario(contexto.getIdUsuario());
        factura.setFecha(LocalDateTime.now());
        factura.setTotal(BigDecimal.ZERO);
        return factura;
    }

    // RN-05 (producto de otra empresa = no encontrado), RN-10 (el precio sale de
    // Producto.precioVenta), RN-01 (el stock se valida solo en decrementarStock)
    // y RN-03 (el subtotal lo calcula el servidor).
    private DetalleFactura armarDetalle(DetalleFacturaRequest linea) {
        ProductoResponse producto = productoService.obtener(linea.idProducto());
        productoService.decrementarStock(linea.idProducto(), linea.cantidad());
        BigDecimal precio = producto.precioVenta();
        DetalleFactura detalle = new DetalleFactura();
        detalle.setIdProducto(linea.idProducto());
        detalle.setCantidad(linea.cantidad());
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(linea.cantidad())));
        return detalle;
    }

    private BigDecimal sumarSubtotales(Factura factura) {
        return factura.getDetalles().stream()
                .map(DetalleFactura::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Factura buscarFactura(Long idFactura) {
        return facturas.findByIdAndIdEmpresa(idFactura, contexto.getIdEmpresa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Factura no encontrada: " + idFactura));
    }
}