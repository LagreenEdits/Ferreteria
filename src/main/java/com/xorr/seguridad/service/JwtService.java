package com.xorr.seguridad.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.xorr.seguridad.entity.Rol;
import com.xorr.seguridad.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final int LONGITUD_MINIMA_SECRETO = 32;
    private static final String ID_USUARIO = "idUsuario";
    private static final String ID_EMPRESA = "idEmpresa";
    private static final String ROL = "rol";

    private final SecretKey clave;
    private final long expiracionMillis;

    public JwtService(@Value("${xorr.jwt.secret}") String secreto,
            @Value("${xorr.jwt.expiracion-minutos}") long expiracionMinutos) {
        validarSecreto(secreto);
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMillis = Duration.ofMinutes(expiracionMinutos).toMillis();
    }

    public String generar(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .claim(ID_USUARIO, usuario.getId())
                .claim(ID_EMPRESA, usuario.getIdEmpresa())
                .claim(ROL, usuario.getRol().name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMillis))
                .signWith(clave)
                .compact();
    }

    // Token vacío, alterado, firmado con otra clave o vencido: todos dan Optional
    // vacío.
    // El filtro (paso 1c) lo traduce a 401; aquí no hace falta distinguir la causa.
    public Optional<DatosToken> leer(String token) {
        try {
            Claims datos = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new DatosToken(
                    numero(datos, ID_USUARIO),
                    numero(datos, ID_EMPRESA),
                    Rol.valueOf((String) datos.get(ROL))));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    // El JSON puede devolver Integer o Long según el tamaño del número.
    private static Long numero(Claims datos, String nombre) {
        return ((Number) datos.get(nombre)).longValue();
    }

    private static void validarSecreto(String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "xorr.jwt.secret debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
    }
}