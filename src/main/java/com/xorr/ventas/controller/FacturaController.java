package com.xorr.ventas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.xorr.ventas.dto.FacturaRequest;
import com.xorr.ventas.dto.FacturaResponse;
import com.xorr.ventas.service.FacturaService;

import jakarta.validation.Valid;

// RN-11: sin PUT ni DELETE a propósito.
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService servicio;

    public FacturaController(FacturaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crear(@Valid @RequestBody FacturaRequest datos) {
        return servicio.crear(datos);
    }

    @GetMapping
    public List<FacturaResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}