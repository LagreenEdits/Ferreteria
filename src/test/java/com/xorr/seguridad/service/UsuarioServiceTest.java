package com.xorr.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.xorr.SesionDePrueba;
import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.Rol;
import com.xorr.seguridad.dto.CrearEmpleadoRequest;
import com.xorr.seguridad.dto.UsuarioResponse;
import com.xorr.seguridad.entity.Usuario;
import com.xorr.seguridad.repository.UsuarioRepository;

// La sesión simulada (empresa 1, usuario 1, ADMIN) la abre SesionDePrueba.
@ExtendWith({ MockitoExtension.class, SesionDePrueba.class })
class UsuarioServiceTest {

    private static final String CLAVE = "clave12345";

    @Mock
    private UsuarioRepository usuarios;

    // BCrypt real: así se comprueba que lo guardado corresponde a la clave.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarios, passwordEncoder, new ContextoActual());
    }

    @Test
    void crearEmpleado_guardaConRolEmpleadoYLaEmpresaDelToken() {
        when(usuarios.existsByCorreo("luis@correo.com")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        servicio.crearEmpleado(new CrearEmpleadoRequest("Luis", "luis@correo.com", CLAVE));

        Usuario guardado = capturarGuardado();
        assertEquals(Rol.EMPLEADO, guardado.getRol());
        assertEquals(1L, guardado.getIdEmpresa());
        assertEquals("Luis", guardado.getNombre());
    }

    @Test
    void crearEmpleado_guardaElHashBCryptYNoLaClave() {
        when(usuarios.existsByCorreo("luis@correo.com")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        servicio.crearEmpleado(new CrearEmpleadoRequest("Luis", "luis@correo.com", CLAVE));

        Usuario guardado = capturarGuardado();
        assertNotEquals(CLAVE, guardado.getPasswordHash());
        assertTrue(passwordEncoder.matches(CLAVE, guardado.getPasswordHash()));
    }

    @Test
    void crearEmpleado_normalizaElCorreo() {
        when(usuarios.existsByCorreo("luis@correo.com")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        servicio.crearEmpleado(new CrearEmpleadoRequest("Luis", "  Luis@Correo.COM ", CLAVE));

        assertEquals("luis@correo.com", capturarGuardado().getCorreo());
    }

    @Test
    void crearEmpleado_correoRepetido_seRechazaYNoGuarda() {
        when(usuarios.existsByCorreo("luis@correo.com")).thenReturn(true);

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> servicio.crearEmpleado(new CrearEmpleadoRequest("Luis", "Luis@correo.com", CLAVE)));

        assertEquals("Ya existe un usuario con el correo luis@correo.com", error.getMessage());
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void crearEmpleado_respondeConLosDatosDelEmpleado() {
        when(usuarios.existsByCorreo("luis@correo.com")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(llamada -> {
            Usuario usuario = llamada.getArgument(0);
            usuario.setId(7L);
            return usuario;
        });

        UsuarioResponse respuesta = servicio.crearEmpleado(new CrearEmpleadoRequest("Luis", "luis@correo.com", CLAVE));

        assertEquals(new UsuarioResponse(7L, "Luis", "luis@correo.com", Rol.EMPLEADO), respuesta);
    }

    @Test
    void listar_consultaSoloLaEmpresaDelToken() {
        Usuario admin = usuario(1L, "Ana", "ana@correo.com", Rol.ADMIN);
        Usuario empleado = usuario(2L, "Luis", "luis@correo.com", Rol.EMPLEADO);
        when(usuarios.findByIdEmpresa(1L)).thenReturn(List.of(admin, empleado));

        List<UsuarioResponse> lista = servicio.listar();

        assertEquals(List.of(
                new UsuarioResponse(1L, "Ana", "ana@correo.com", Rol.ADMIN),
                new UsuarioResponse(2L, "Luis", "luis@correo.com", Rol.EMPLEADO)), lista);
    }

    private Usuario capturarGuardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        return captor.getValue();
    }

    private static Usuario usuario(Long id, String nombre, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setIdEmpresa(1L);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPasswordHash("hash-que-no-debe-salir");
        usuario.setRol(rol);
        return usuario;
    }
}