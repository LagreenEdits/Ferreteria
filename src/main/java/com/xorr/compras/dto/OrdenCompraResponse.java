package com.xorr.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.xorr.compras.entity.EstadoOrdenCompra;
import com.xorr.compras.entity.OrdenCompra;

public record OrdenCompraResponse(
        Long id,
        Long idProveedor,
        Long idUsuario,
        LocalDateTime fecha,
        EstadoOrdenCompra estado,
        BigDecimal total,
        List<DetalleOrdenCompraResponse> detalles) {

    public static OrdenCompraResponse desde(OrdenCompra orden) {
        return new OrdenCompraResponse(
                orden.getId(),
                orden.getIdProveedor(),
                orden.getIdUsuario(),
                orden.getFecha(),
                orden.getEstado(),
                orden.getTotal(),
                orden.getDetalles().stream().map(DetalleOrdenCompraResponse::desde).toList());
    }
}