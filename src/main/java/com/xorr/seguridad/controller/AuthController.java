package com.xorr.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xorr.seguridad.dto.LoginRequest;
import com.xorr.seguridad.dto.LoginResponse;
import com.xorr.seguridad.dto.RegistroAdminRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro-admin")
    public ResponseEntity<UsuarioResponse> registrarAdmin(@Valid @RequestBody RegistroAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarAdmin(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}