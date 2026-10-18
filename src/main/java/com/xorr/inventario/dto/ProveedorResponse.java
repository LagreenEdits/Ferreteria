package com.xorr.inventario.dto;

import com.xorr.inventario.entity.Proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String telefono) {

    public static ProveedorResponse desde(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getTelefono());
    }
}