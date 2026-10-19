package com.xorr.seguridad.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.xorr.seguridad.service.DatosToken;
import com.xorr.seguridad.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Si la petición trae "Authorization: Bearer <token>" y el token es válido,
 * deja
 * al usuario autenticado. Si no trae token o es inválido, NO responde nada:
 * sigue
 * sin sesión y las reglas de SecurityConfig deciden (401 en rutas protegidas).
 * No es @Component a propósito: SecurityConfig lo crea y lo coloca en la
 * cadena.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        extraerToken(request).flatMap(jwtService::leer).ifPresent(JwtAuthenticationFilter::autenticar);
        cadena.doFilter(request, response);
    }

    private static Optional<String> extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return Optional.empty();
        }
        return Optional.of(cabecera.substring(PREFIJO.length()).trim());
    }

    private static void autenticar(DatosToken datos) {
        SimpleGrantedAuthority rol = new SimpleGrantedAuthority("ROLE_" + datos.rol().name());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(datos, null, List.of(rol)));
    }
}