package com.xorr;

import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.xorr.seguridad.entity.Rol;
import com.xorr.seguridad.service.DatosToken;

// Las pruebas unitarias de los servicios no pasan por el filtro JWT, así que
// simulan la sesión que este crearía: usuario 1, empresa 1, rol ADMIN.
public class SesionDePrueba implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        DatosToken datos = new DatosToken(1L, 1L, Rol.ADMIN);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(datos, null, List.of()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        SecurityContextHolder.clearContext();
    }
}