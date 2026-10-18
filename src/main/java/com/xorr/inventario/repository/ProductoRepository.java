package com.xorr.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xorr.inventario.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<Producto> findByIdEmpresa(Long idEmpresa);

    @Query("select p from Producto p where p.idEmpresa = :idEmpresa and p.stock < p.stockMinimo")
    List<Producto> buscarConStockBajo(@Param("idEmpresa") Long idEmpresa);
}