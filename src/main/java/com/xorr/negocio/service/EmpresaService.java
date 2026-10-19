package com.xorr.negocio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.negocio.entity.Empresa;
import com.xorr.negocio.repository.EmpresaRepository;

@Service
public class EmpresaService {

    private final EmpresaRepository empresas;

    public EmpresaService(EmpresaRepository empresas) {
        this.empresas = empresas;
    }

    // Devuelve solo el id: Seguridad no necesita la entidad de Negocio (4.4).
    @Transactional
    public Long crear(String nombre) {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        return empresas.save(empresa).getId();
    }
}