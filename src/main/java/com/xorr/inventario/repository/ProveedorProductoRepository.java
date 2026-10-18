package com.xorr.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.inventario.entity.ProveedorProducto;

public interface ProveedorProductoRepository extends JpaRepository<ProveedorProducto, Long> {

    Optional<ProveedorProducto> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<ProveedorProducto> findByIdEmpresa(Long idEmpresa);
}