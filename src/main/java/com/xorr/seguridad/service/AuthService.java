package com.xorr.seguridad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.CredencialesInvalidasException;
import com.xorr.common.Rol;
import com.xorr.negocio.service.EmpresaService;
import com.xorr.seguridad.dto.LoginRequest;
import com.xorr.seguridad.dto.LoginResponse;
import com.xorr.seguridad.dto.RegistroAdminRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.entity.Usuario;
import com.xorr.seguridad.repository.UsuarioRepository;

@Service
public class AuthService {

    // Mismo mensaje si falla el correo o la contraseña: no revela cuál de los dos
    // existe.
    private static final String MENSAJE_LOGIN_INVALIDO = "Correo o contraseña incorrectos";

    private final UsuarioRepository usuarios;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, EmpresaService empresaService, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarios = usuarios;
        this.empresaService = empresaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Empresa y admin se crean juntos: si algo falla, no queda ninguno (RN-06).
    @Transactional
    public UsuarioResponse registrarAdmin(RegistroAdminRequest request) {
        String correo = CorreoUsuario.normalizar(request.correo());
        CorreoUsuario.validarLibre(usuarios, correo);
        Long idEmpresa = empresaService.crear(request.nombreEmpresa());
        return UsuarioResponse.desde(usuarios.save(nuevoAdmin(idEmpresa, correo, request)));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByCorreo(CorreoUsuario.normalizar(request.correo()))
                .filter(encontrado -> passwordEncoder.matches(request.password(), encontrado.getPasswordHash()))
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_LOGIN_INVALIDO));
        return new LoginResponse(jwtService.generar(usuario), UsuarioResponse.desde(usuario));
    }

    private Usuario nuevoAdmin(Long idEmpresa, String correo, RegistroAdminRequest request) {
        Usuario admin = new Usuario();
        admin.setIdEmpresa(idEmpresa);
        admin.setNombre(request.nombre());
        admin.setCorreo(correo);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRol(Rol.ADMIN);
        return admin;
    }
}