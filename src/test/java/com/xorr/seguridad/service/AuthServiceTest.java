package com.xorr.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.xorr.common.CredencialesInvalidasException;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.Rol;
import com.xorr.negocio.service.EmpresaService;
import com.xorr.seguridad.dto.LoginRequest;
import com.xorr.seguridad.dto.LoginResponse;
import com.xorr.seguridad.dto.RegistroAdminRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.entity.Usuario;
import com.xorr.seguridad.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CLAVE = "clave12345";

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private EmpresaService empresaService;
    @Mock
    private JwtService jwtService;
    // Encoder real (no mock): así se comprueba que el hash de verdad corresponde a
    // la clave.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarios, empresaService, encoder, jwtService);
    }

    private static RegistroAdminRequest pedido(String correo) {
        return new RegistroAdminRequest("Ferretería El Tornillo", "Ana Pérez", correo, CLAVE);
    }

    private Usuario registrarYCapturar(String correoEnviado, String correoNormalizado) {
        when(usuarios.existsByCorreo(correoNormalizado)).thenReturn(false);
        when(empresaService.crear("Ferretería El Tornillo")).thenReturn(7L);
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrarAdmin(pedido(correoEnviado));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        return captor.getValue();
    }

    private Usuario usuarioGuardado() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setIdEmpresa(7L);
        usuario.setNombre("Ana Pérez");
        usuario.setCorreo("ana@correo.com");
        usuario.setPasswordHash(encoder.encode(CLAVE));
        usuario.setRol(Rol.ADMIN);
        return usuario;
    }

    // ---------- registrarAdmin ----------

    @Test
    void registrarAdmin_creaElUsuarioEnLaEmpresaNuevaConRolAdmin() {
        Usuario guardado = registrarYCapturar("ana@correo.com", "ana@correo.com");

        assertEquals(7L, guardado.getIdEmpresa());
        assertEquals(Rol.ADMIN, guardado.getRol());
        assertEquals("Ana Pérez", guardado.getNombre());
        assertEquals("ana@correo.com", guardado.getCorreo());
    }

    @Test
    void registrarAdmin_guardaUnHashBCryptYNuncaLaContrasenaEnClaro() {
        Usuario guardado = registrarYCapturar("ana@correo.com", "ana@correo.com");

        assertNotEquals(CLAVE, guardado.getPasswordHash());
        assertTrue(encoder.matches(CLAVE, guardado.getPasswordHash()));
    }

    @Test
    void registrarAdmin_normalizaElCorreoAMinusculas() {
        Usuario guardado = registrarYCapturar("ANA@Correo.COM", "ana@correo.com");

        assertEquals("ana@correo.com", guardado.getCorreo());
    }

    @Test
    void registrarAdmin_devuelveElUsuarioSinDatosSensibles() {
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(false);
        when(empresaService.crear("Ferretería El Tornillo")).thenReturn(7L);
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioResponse respuesta = servicio.registrarAdmin(pedido("ana@correo.com"));

        assertEquals("ana@correo.com", respuesta.correo());
        assertEquals(Rol.ADMIN, respuesta.rol());
    }

    @Test
    void registrarAdmin_conCorreoYaRegistrado_seRechazaYNoCreaNada() {
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(true);
        RegistroAdminRequest pedido = pedido("ana@correo.com");

        assertThrows(OperacionInvalidaException.class, () -> servicio.registrarAdmin(pedido));

        verify(empresaService, never()).crear(any());
        verify(usuarios, never()).save(any());
    }

    @Test
    void registrarAdmin_conCorreoYaRegistradoEnOtraMayuscula_tambienSeRechaza() {
        when(usuarios.existsByCorreo("ana@correo.com")).thenReturn(true);
        RegistroAdminRequest pedido = pedido("ANA@correo.com");

        assertThrows(OperacionInvalidaException.class, () -> servicio.registrarAdmin(pedido));
    }

    // ---------- login ----------

    @Test
    void login_conCredencialesCorrectas_devuelveElTokenYElUsuario() {
        Usuario usuario = usuarioGuardado();
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generar(usuario)).thenReturn("token-de-prueba");

        LoginResponse respuesta = servicio.login(new LoginRequest("ana@correo.com", CLAVE));

        assertEquals("token-de-prueba", respuesta.token());
        assertEquals("ana@correo.com", respuesta.usuario().correo());
        assertEquals(Rol.ADMIN, respuesta.usuario().rol());
    }

    @Test
    void login_normalizaElCorreoAntesDeBuscar() {
        Usuario usuario = usuarioGuardado();
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generar(usuario)).thenReturn("token-de-prueba");

        LoginResponse respuesta = servicio.login(new LoginRequest("ANA@Correo.COM", CLAVE));

        assertEquals("token-de-prueba", respuesta.token());
    }

    @Test
    void login_conCorreoInexistente_seRechazaYNoGeneraToken() {
        when(usuarios.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());
        LoginRequest pedido = new LoginRequest("nadie@correo.com", CLAVE);

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(pedido));

        verify(jwtService, never()).generar(any());
    }

    @Test
    void login_conContrasenaIncorrecta_seRechazaYNoGeneraToken() {
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuarioGuardado()));
        LoginRequest pedido = new LoginRequest("ana@correo.com", "otraClave99");

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(pedido));

        verify(jwtService, never()).generar(any());
    }

    @Test
    void login_elMensajeDeErrorEsElMismoParaCorreoInexistenteYContrasenaIncorrecta() {
        when(usuarios.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@correo.com")).thenReturn(Optional.of(usuarioGuardado()));
        LoginRequest sinCorreo = new LoginRequest("nadie@correo.com", CLAVE);
        LoginRequest sinClave = new LoginRequest("ana@correo.com", "otraClave99");

        String mensajeCorreo = assertThrows(CredencialesInvalidasException.class, () -> servicio.login(sinCorreo))
                .getMessage();
        String mensajeClave = assertThrows(CredencialesInvalidasException.class, () -> servicio.login(sinClave))
                .getMessage();

        assertEquals(mensajeCorreo, mensajeClave);
    }
}