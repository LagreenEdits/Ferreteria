package com.xorr.common;

import org.springframework.stereotype.Component;

/**
 * Única fuente del idEmpresa y del idUsuario. En la Fase 0 son fijos; más
 * adelante
 * se reemplaza por los datos del JWT sin tocar el resto del código.
 */
@Component
public class ContextoActual {

    private static final long ID_EMPRESA_FIJA = 1L;
    private static final long ID_USUARIO_FIJO = 1L;

    public Long getIdEmpresa() {
        return ID_EMPRESA_FIJA;
    }

    public Long getIdUsuario() {
        return ID_USUARIO_FIJO;
    }
}