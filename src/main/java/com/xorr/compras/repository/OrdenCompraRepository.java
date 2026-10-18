package com.xorr.compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.compras.entity.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<OrdenCompra> findByIdEmpresa(Long idEmpresa);
}