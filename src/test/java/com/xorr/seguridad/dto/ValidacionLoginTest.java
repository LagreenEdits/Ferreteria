package com.xorr.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ValidacionLoginTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(LoginRequest request) {
        return validador.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void pedidoCompleto_noTieneErrores() {
        assertTrue(camposInvalidos(new LoginRequest("ana@correo.com", "clave12345")).isEmpty());
    }

    @Test
    void correoVacio_seRechaza() {
        assertEquals(Set.of("correo"), camposInvalidos(new LoginRequest(" ", "clave12345")));
    }

    @Test
    void contrasenaNula_seRechaza() {
        assertEquals(Set.of("password"), camposInvalidos(new LoginRequest("ana@correo.com", null)));
    }

    @Test
    void ambosCamposNulos_seRechazanLosDos() {
        assertEquals(Set.of("correo", "password"), camposInvalidos(new LoginRequest(null, null)));
    }
}