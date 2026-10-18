package com.xorr.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.common.ContextoActual;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProveedorRequest;
import com.xorr.inventario.dto.ProveedorResponse;
import com.xorr.inventario.entity.Proveedor;
import com.xorr.inventario.repository.ProveedorProductoRepository;
import com.xorr.inventario.repository.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_PROVEEDOR = 20L;

    @Mock
    private ProveedorRepository repositorio;

    @Mock
    private ProveedorProductoRepository asociaciones;

    private ProveedorService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProveedorService(repositorio, asociaciones, new ContextoActual());
    }

    @Test
    void crear_asignaLaEmpresaActualYGuarda() {
        when(repositorio.save(any(Proveedor.class))).thenAnswer(llamada -> llamada.getArgument(0));

        ProveedorResponse respuesta = servicio.crear(new ProveedorRequest("Ferrelectro", "3001234567"));

        ArgumentCaptor<Proveedor> guardado = ArgumentCaptor.forClass(Proveedor.class);
        verify(repositorio).save(guardado.capture());
        assertEquals(ID_EMPRESA, guardado.getValue().getIdEmpresa());
        assertEquals("Ferrelectro", respuesta.nombre());
    }

    @Test
    void listar_consultaSoloLaEmpresaActual() {
        when(repositorio.findByIdEmpresa(ID_EMPRESA)).thenReturn(List.of(proveedorPropio()));

        List<ProveedorResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals("Ferrelectro", resultado.get(0).nombre());
    }

    @Test
    void actualizar_conProveedorPropio_cambiaLosDatos() {
        Proveedor proveedor = proveedorPropio();
        when(repositorio.findByIdAndIdEmpresa(ID_PROVEEDOR, ID_EMPRESA)).thenReturn(Optional.of(proveedor));
        when(repositorio.save(proveedor)).thenReturn(proveedor);

        servicio.actualizar(ID_PROVEEDOR, new ProveedorRequest("Ferretodo", "3109998888"));

        assertEquals("Ferretodo", proveedor.getNombre());
        assertEquals("3109998888", proveedor.getTelefono());
    }

    @Test
    void eliminar_conProveedorPropio_loEliminaJuntoConSusAsociaciones() {
        Proveedor proveedor = proveedorPropio();
        when(repositorio.findByIdAndIdEmpresa(ID_PROVEEDOR, ID_EMPRESA)).thenReturn(Optional.of(proveedor));

        servicio.eliminar(ID_PROVEEDOR);

        verify(asociaciones).deleteByIdEmpresaAndIdProveedor(ID_EMPRESA, ID_PROVEEDOR);
        verify(repositorio).delete(proveedor);
    }

    @Test
    void obtener_proveedorDeOtraEmpresa_seTrataComoNoEncontrado() {
        proveedorEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(ID_PROVEEDOR));
    }

    @Test
    void actualizar_proveedorDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        proveedorEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizar(ID_PROVEEDOR, new ProveedorRequest("Ferretodo", null)));

        verify(repositorio, never()).save(any());
    }

    @Test
    void eliminar_proveedorDeOtraEmpresa_seTrataComoNoEncontradoYNoBorraNada() {
        proveedorEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(ID_PROVEEDOR));

        verify(repositorio, never()).delete(any());
        verifyNoInteractions(asociaciones);
    }

    private Proveedor proveedorPropio() {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdEmpresa(ID_EMPRESA);
        proveedor.setNombre("Ferrelectro");
        proveedor.setTelefono("3001234567");
        return proveedor;
    }

    private void proveedorEsDeOtraEmpresa() {
        when(repositorio.findByIdAndIdEmpresa(ID_PROVEEDOR, ID_EMPRESA)).thenReturn(Optional.empty());
    }
}