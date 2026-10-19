package com.xorr.negocio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.negocio.entity.Empresa;
import com.xorr.negocio.repository.EmpresaRepository;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresas;
    @InjectMocks
    private EmpresaService servicio;

    @Test
    void crear_guardaLaEmpresaConSuNombreYDevuelveSuId() {
        when(empresas.save(any(Empresa.class))).thenAnswer(invocacion -> {
            Empresa guardada = invocacion.getArgument(0);
            guardada.setId(5L);
            return guardada;
        });

        Long id = servicio.crear("Ferretería El Tornillo");

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresas).save(captor.capture());
        assertEquals("Ferretería El Tornillo", captor.getValue().getNombre());
        assertEquals(5L, id);
    }
}