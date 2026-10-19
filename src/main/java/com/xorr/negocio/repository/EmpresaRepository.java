package com.xorr.negocio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xorr.negocio.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}