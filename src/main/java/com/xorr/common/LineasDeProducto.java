package com.xorr.common;

import java.util.List;

public final class LineasDeProducto {

    private LineasDeProducto() {
    }

    public static void validarSinRepetidos(List<Long> idsProducto, String mensaje) {
        if (idsProducto.stream().distinct().count() != idsProducto.size()) {
            throw new OperacionInvalidaException(mensaje);
        }
    }
}