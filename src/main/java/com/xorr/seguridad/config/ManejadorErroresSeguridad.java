package com.xorr.seguridad.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Los errores de seguridad ocurren en el filtro, antes del controlador, así que
 * GlobalExceptionHandler no los ve. Esta clase los devuelve con el mismo
 * formato
 * JSON del resto de la API: {"mensaje": "..."}.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    // 401: no hay token, es inválido o está vencido.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión con un token válido");
    }

    // 403: hay sesión, pero el rol no alcanza para esta operación.
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tienes permiso para esta operación");
    }

    private static void escribir(HttpServletResponse response, HttpStatus estado, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}