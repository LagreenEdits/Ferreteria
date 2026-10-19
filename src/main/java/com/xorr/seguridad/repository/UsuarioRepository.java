package com.xorr.seguridad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.seguridad.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    // Lo usa el login.
    Optional<Usuario> findByCorreo(String correo);

    // Lo usa el listado de usuarios: solo los de la empresa del token (RN-05).
    List<Usuario> findByIdEmpresa(Long idEmpresa);
}