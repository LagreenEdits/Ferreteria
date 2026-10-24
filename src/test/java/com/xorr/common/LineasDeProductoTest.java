package com.xorr.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class LineasDeProductoTest {

    @Test
    void validarSinRepetidos_conIdsDistintos_noLanza() {
        assertDoesNotThrow(() -> LineasDeProducto.validarSinRepetidos(List.of(1L, 2L, 3L), "mensaje"));
    }

    @Test
    void validarSinRepetidos_conIdsRepetidos_lanzaConElMensajeRecibido() {
        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> LineasDeProducto.validarSinRepetidos(List.of(1L, 2L, 1L), "repetido"));
        assertEquals("repetido", error.getMessage());
    }
}