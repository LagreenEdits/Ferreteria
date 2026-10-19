package com.xorr.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

// Bean Validation real, sin Spring (igual que ValidacionDtoTest): Mockito no ejecuta estas anotaciones.
class ValidacionRegistroAdminTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(RegistroAdminRequest request) {
        return validador.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void pedidoCompleto_noTieneErrores() {
        assertTrue(camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "ana@correo.com", "clave12345")).isEmpty());
    }

    @Test
    void nombreDeEmpresaVacio_seRechaza() {
        assertEquals(Set.of("nombreEmpresa"),
                camposInvalidos(new RegistroAdminRequest(" ", "Ana", "ana@correo.com", "clave12345")));
    }

    @Test
    void nombreNulo_seRechaza() {
        assertEquals(Set.of("nombre"),
                camposInvalidos(new RegistroAdminRequest("Ferre", null, "ana@correo.com", "clave12345")));
    }

    @Test
    void correoSinFormato_seRechaza() {
        assertEquals(Set.of("correo"),
                camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "no-es-un-correo", "clave12345")));
    }

    @Test
    void correoVacio_seRechaza() {
        assertTrue(camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "", "clave12345")).contains("correo"));
    }

    @Test
    void contrasenaDeSieteCaracteres_seRechaza() {
        assertEquals(Set.of("password"),
                camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "ana@correo.com", "1234567")));
    }

    @Test
    void contrasenaDeOchoCaracteres_seAcepta() {
        assertTrue(camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "ana@correo.com", "12345678")).isEmpty());
    }

    @Test
    void contrasenaDeMasDe72Caracteres_seRechaza() {
        assertEquals(Set.of("password"),
                camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "ana@correo.com", "x".repeat(73))));
    }

    @Test
    void contrasenaNula_seRechaza() {
        assertEquals(Set.of("password"),
                camposInvalidos(new RegistroAdminRequest("Ferre", "Ana", "ana@correo.com", null)));
    }
}