package com.xorr.common;

// Login fallido. Se traduce a 401 en GlobalExceptionHandler.
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}