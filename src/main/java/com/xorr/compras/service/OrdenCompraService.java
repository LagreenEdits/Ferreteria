package com.xorr.compras.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.compras.dto.DetalleOrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraResponse;
import com.xorr.compras.entity.DetalleOrdenCompra;
import com.xorr.compras.entity.EstadoOrdenCompra;
import com.xorr.compras.entity.OrdenCompra;
import com.xorr.compras.repository.OrdenCompraRepository;
import com.xorr.inventario.service.ProductoService;
import com.xorr.inventario.service.ProveedorProductoService;
import com.xorr.inventario.service.ProveedorService;

@Service
public class OrdenCompraService {

    private final OrdenCompraRepository ordenes;
    private final ContextoActual contexto;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final ProveedorProductoService proveedorProductoService;

    public OrdenCompraService(OrdenCompraRepository ordenes,
            ContextoActual contexto,
            ProveedorService proveedorService,
            ProductoService productoService,
            ProveedorProductoService proveedorProductoService) {
        this.ordenes = ordenes;
        this.contexto = contexto;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.proveedorProductoService = proveedorProductoService;
    }

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest datos) {
        proveedorService.obtener(datos.idProveedor());
        OrdenCompra orden = nuevaOrdenPendiente(datos.idProveedor());
        datos.detalles().forEach(linea -> orden.getDetalles().add(armarDetalle(datos.idProveedor(), linea)));
        orden.setTotal(sumarSubtotales(orden));
        return OrdenCompraResponse.desde(ordenes.save(orden));
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

    private OrdenCompra nuevaOrdenPendiente(Long idProveedor) {
        OrdenCompra orden = new OrdenCompra();
        orden.setIdEmpresa(contexto.getIdEmpresa());
        orden.setIdProveedor(idProveedor);
        orden.setIdUsuario(contexto.getIdUsuario());
        orden.setFecha(LocalDateTime.now());
        orden.setEstado(EstadoOrdenCompra.PENDIENTE);
        orden.setTotal(BigDecimal.ZERO);
        return orden;
    }

    // RN-05 (producto de otra empresa = no encontrado), RN-09 (precio de la
    // asociación)
    // y RN-03 (el subtotal lo calcula el servidor).
    private DetalleOrdenCompra armarDetalle(Long idProveedor, DetalleOrdenCompraRequest linea) {
        productoService.obtener(linea.idProducto());
        BigDecimal precio = proveedorProductoService.obtenerPrecioCompra(idProveedor, linea.idProducto());
        DetalleOrdenCompra detalle = new DetalleOrdenCompra();
        detalle.setIdProducto(linea.idProducto());
        detalle.setCantidad(linea.cantidad());
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(linea.cantidad())));
        return detalle;
    }

    private BigDecimal sumarSubtotales(OrdenCompra orden) {
        return orden.getDetalles().stream()
                .map(DetalleOrdenCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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