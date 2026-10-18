package com.xorr.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.inventario.entity.ProveedorProducto;

public interface ProveedorProductoRepository extends JpaRepository<ProveedorProducto, Long> {

    Optional<ProveedorProducto> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    Optional<ProveedorProducto> findByIdEmpresaAndIdProveedorAndIdProducto(
            Long idEmpresa, Long idProveedor, Long idProducto);

    List<ProveedorProducto> findByIdEmpresa(Long idEmpresa);

    boolean existsByIdEmpresaAndIdProveedorAndIdProducto(Long idEmpresa, Long idProveedor, Long idProducto);

    boolean existsByIdEmpresaAndIdProveedorAndIdProductoAndIdNot(
            Long idEmpresa, Long idProveedor, Long idProducto, Long id);

    void deleteByIdEmpresaAndIdProducto(Long idEmpresa, Long idProducto);

    void deleteByIdEmpresaAndIdProveedor(Long idEmpresa, Long idProveedor);
}