package com.xorr.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.xorr.inventario.dto.ProveedorProductoRequest;
import com.xorr.inventario.dto.ProveedorProductoResponse;
import com.xorr.inventario.service.ProveedorProductoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/proveedor-productos")
public class ProveedorProductoController {

    private final ProveedorProductoService servicio;

    public ProveedorProductoController(ProveedorProductoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorProductoResponse crear(@Valid @RequestBody ProveedorProductoRequest datos) {
        return servicio.crear(datos);
    }

    @GetMapping
    public List<ProveedorProductoResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public ProveedorProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}")
    public ProveedorProductoResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody ProveedorProductoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}