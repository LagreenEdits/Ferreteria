package com.xorr.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

// Validación real de Bean Validation, sin Spring (RN-07).
class ValidacionCrearEmpleadoRequestTest {

    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        validador = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void datosCorrectos_noTienenErrores() {
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", "luis@correo.com", "clave12345")).isEmpty());
    }

    @Test
    void contrasenaEnLosLimites_8y72_esValida() {
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", "luis@correo.com", "a".repeat(8))).isEmpty());
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", "luis@correo.com", "a".repeat(72))).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void nombreVacioONulo_seRechaza(String nombre) {
        assertTrue(
                camposInvalidos(new CrearEmpleadoRequest(nombre, "luis@correo.com", "clave12345")).contains("nombre"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "luis-sin-arroba" })
    void correoVacioNuloOMalEscrito_seRechaza(String correo) {
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", correo, "clave12345")).contains("correo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "1234567" })
    void contrasenaVaciaNulaOCorta_seRechaza(String password) {
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", "luis@correo.com", password)).contains("password"));
    }

    @Test
    void contrasenaDeMasDe72Caracteres_seRechaza() {
        assertTrue(camposInvalidos(new CrearEmpleadoRequest("Luis", "luis@correo.com", "a".repeat(73)))
                .contains("password"));
    }

    private static Set<String> camposInvalidos(CrearEmpleadoRequest request) {
        return validador.validate(request).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}