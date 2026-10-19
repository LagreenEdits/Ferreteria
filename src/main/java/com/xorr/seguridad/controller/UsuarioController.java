package com.xorr.seguridad.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xorr.seguridad.dto.CrearEmpleadoRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.service.UsuarioService;

import jakarta.validation.Valid;

// Solo ADMIN: lo exige SecurityConfig.
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crearEmpleado(@Valid @RequestBody CrearEmpleadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearEmpleado(request));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }
}