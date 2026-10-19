package com.xorr.seguridad.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.seguridad.dto.CrearEmpleadoRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.entity.Rol;
import com.xorr.seguridad.entity.Usuario;
import com.xorr.seguridad.repository.UsuarioRepository;

/**
 * Alta y consulta de empleados por parte del ADMIN. El rol lo decide el
 * servicio (siempre EMPLEADO) y la empresa sale del token (RN-05).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final ContextoActual contexto;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, ContextoActual contexto) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.contexto = contexto;
    }

    @Transactional
    public UsuarioResponse crearEmpleado(CrearEmpleadoRequest request) {
        String correo = normalizar(request.correo());
        validarCorreoLibre(correo);
        return UsuarioResponse.desde(usuarios.save(nuevoEmpleado(correo, request)));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findByIdEmpresa(contexto.getIdEmpresa()).stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    // Misma regla que AuthService: "Ana@x.com" y "ana@x.com" son el mismo correo.
    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void validarCorreoLibre(String correo) {
        if (usuarios.existsByCorreo(correo)) {
            throw new OperacionInvalidaException("Ya existe un usuario con el correo " + correo);
        }
    }

    private Usuario nuevoEmpleado(String correo, CrearEmpleadoRequest request) {
        Usuario empleado = new Usuario();
        empleado.setIdEmpresa(contexto.getIdEmpresa());
        empleado.setNombre(request.nombre());
        empleado.setCorreo(correo);
        empleado.setPasswordHash(passwordEncoder.encode(request.password()));
        empleado.setRol(Rol.EMPLEADO);
        return empleado;
    }
}