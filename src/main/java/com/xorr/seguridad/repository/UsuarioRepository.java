package com.xorr.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.seguridad.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    // Lo usará el login (paso 1b).
    Optional<Usuario> findByCorreo(String correo);
}