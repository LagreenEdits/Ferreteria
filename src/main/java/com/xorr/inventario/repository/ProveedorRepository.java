package com.xorr.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.inventario.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<Proveedor> findByIdEmpresa(Long idEmpresa);
}