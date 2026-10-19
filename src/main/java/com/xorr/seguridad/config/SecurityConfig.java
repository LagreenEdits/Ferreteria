package com.xorr.seguridad.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.xorr.seguridad.service.JwtService;

/**
 * Quién puede llamar a qué (sección 6 del documento maestro).
 * Se usa AntPathRequestMatcher a propósito: con la consola H2 hay dos servlets
 * registrados y requestMatchers("texto") no sabe decidir entre ellos y falla al
 * arrancar.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private static final String ADMIN = "ADMIN";
        private static final String EMPLEADO = "EMPLEADO";

        @Bean
        public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtService jwtService,
                        ManejadorErroresSeguridad errores) throws Exception {
                http
                                // Sin cookies ni sesión: cada petición trae su token, así que CSRF no aplica.
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(
                                                sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // La consola H2 se dibuja dentro de marcos de la misma página.
                                .headers(cabeceras -> cabeceras.frameOptions(marco -> marco.sameOrigin()))
                                .exceptionHandling(
                                                e -> e.authenticationEntryPoint(errores).accessDeniedHandler(errores))
                                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                                                UsernamePasswordAuthenticationFilter.class)
                                .authorizeHttpRequests(reglas -> reglas
                                                // Públicas: registro y login.
                                                .requestMatchers(rutas(HttpMethod.POST, "/api/auth/registro-admin",
                                                                "/api/auth/login"))
                                                .permitAll()
                                                // Públicas: la página del frontend
                                                // (src/main/resources/static/index.html).
                                                // No expone datos: todas sus llamadas a /api/** llevan el token.
                                                .requestMatchers(rutas(HttpMethod.GET, "/", "/index.html"))
                                                .permitAll()
                                                // Públicas: /error (Spring responde 405 y otros errores a través de
                                                // ella),
                                                // documentación y consola H2. Las dos últimas, solo para desarrollo.
                                                .requestMatchers(rutas("/error", "/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**", "/h2-console/**"))
                                                .permitAll()
                                                // Solo ADMIN: empleados, proveedores, asociaciones y órdenes de compra.
                                                .requestMatchers(rutas("/api/usuarios/**", "/api/proveedores/**",
                                                                "/api/proveedor-productos/**",
                                                                "/api/ordenes-compra/**"))
                                                .hasRole(ADMIN)
                                                // Productos: leer, ADMIN y EMPLEADO; escribir, solo ADMIN.
                                                .requestMatchers(rutas(HttpMethod.GET, "/api/productos/**"))
                                                .hasAnyRole(ADMIN, EMPLEADO)
                                                .requestMatchers(rutas("/api/productos/**"))
                                                .hasRole(ADMIN)
                                                // Ventas: ADMIN y EMPLEADO.
                                                .requestMatchers(rutas("/api/facturas/**"))
                                                .hasAnyRole(ADMIN, EMPLEADO)
                                                // Cualquier otra ruta se deniega.
                                                .anyRequest().denyAll());
                return http.build();
        }

        private static RequestMatcher[] rutas(String... patrones) {
                return Arrays.stream(patrones)
                                .map(patron -> new AntPathRequestMatcher(patron))
                                .toArray(RequestMatcher[]::new);
        }

        private static RequestMatcher[] rutas(HttpMethod metodo, String... patrones) {
                return Arrays.stream(patrones)
                                .map(patron -> new AntPathRequestMatcher(patron, metodo.name()))
                                .toArray(RequestMatcher[]::new);
        }
}