package com.xorr.compras.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.xorr.compras.dto.OrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraResponse;
import com.xorr.compras.service.OrdenCompraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordenes-compra")
public class OrdenCompraController {

    private final OrdenCompraService servicio;

    public OrdenCompraController(OrdenCompraService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenCompraResponse crear(@Valid @RequestBody OrdenCompraRequest datos) {
        return servicio.crear(datos);
    }

    @GetMapping
    public List<OrdenCompraResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public OrdenCompraResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}/recibir")
    public OrdenCompraResponse recibir(@PathVariable Long id) {
        return servicio.recibir(id);
    }

    @PutMapping("/{id}/cancelar")
    public OrdenCompraResponse cancelar(@PathVariable Long id) {
        return servicio.cancelar(id);
    }
}