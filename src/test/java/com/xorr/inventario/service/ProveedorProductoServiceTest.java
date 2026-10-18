package com.xorr.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProveedorProductoRequest;
import com.xorr.inventario.dto.ProveedorProductoResponse;
import com.xorr.inventario.entity.ProveedorProducto;
import com.xorr.inventario.repository.ProveedorProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorProductoServiceTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_ASOCIACION = 30L;
    private static final Long ID_PROVEEDOR = 20L;
    private static final Long ID_PRODUCTO = 10L;

    @Mock
    private ProveedorProductoRepository repositorio;

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private ProductoService productoService;

    private ProveedorProductoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProveedorProductoService(
                repositorio, new ContextoActual(), proveedorService, productoService);
    }

    // ---- crear ----

    @Test
    void crear_conProveedorYProductoPropios_guardaConLaEmpresaActual() {
        when(repositorio.save(any(ProveedorProducto.class))).thenAnswer(llamada -> llamada.getArgument(0));

        ProveedorProductoResponse respuesta = servicio.crear(solicitud("9000"));

        ArgumentCaptor<ProveedorProducto> guardada = ArgumentCaptor.forClass(ProveedorProducto.class);
        verify(repositorio).save(guardada.capture());
        assertEquals(ID_EMPRESA, guardada.getValue().getIdEmpresa());
        assertEquals(ID_PROVEEDOR, guardada.getValue().getIdProveedor());
        assertEquals(ID_PRODUCTO, guardada.getValue().getIdProducto());
        assertEquals(new BigDecimal("9000"), respuesta.precioCompra());
        verify(proveedorService).obtener(ID_PROVEEDOR);
        verify(productoService).obtener(ID_PRODUCTO);
    }

    @Test
    void crear_conProveedorDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        when(proveedorService.obtener(ID_PROVEEDOR))
                .thenThrow(new RecursoNoEncontradoException("Proveedor no encontrado"));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(solicitud("9000")));

        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_conProductoDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        when(productoService.obtener(ID_PRODUCTO))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(solicitud("9000")));

        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_conAsociacionDuplicada_seRechazaYNoGuarda() {
        when(repositorio.existsByIdEmpresaAndIdProveedorAndIdProducto(ID_EMPRESA, ID_PROVEEDOR, ID_PRODUCTO))
                .thenReturn(true);

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> servicio.crear(solicitud("9000")));

        assertEquals("Ya existe esa asociación", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    // ---- actualizar ----

    @Test
    void actualizar_conAsociacionPropia_cambiaElPrecio() {
        ProveedorProducto asociacion = asociacionPropia();
        when(repositorio.findByIdAndIdEmpresa(ID_ASOCIACION, ID_EMPRESA)).thenReturn(Optional.of(asociacion));
        when(repositorio.save(asociacion)).thenReturn(asociacion);

        servicio.actualizar(ID_ASOCIACION, solicitud("12000"));

        assertEquals(new BigDecimal("12000"), asociacion.getPrecioCompra());
    }

    @Test
    void actualizar_haciaUnaParejaQueYaExiste_seRechazaYNoGuarda() {
        ProveedorProducto asociacion = asociacionPropia();
        when(repositorio.findByIdAndIdEmpresa(ID_ASOCIACION, ID_EMPRESA)).thenReturn(Optional.of(asociacion));
        when(repositorio.existsByIdEmpresaAndIdProveedorAndIdProductoAndIdNot(
                ID_EMPRESA, ID_PROVEEDOR, ID_PRODUCTO, ID_ASOCIACION)).thenReturn(true);

        assertThrows(OperacionInvalidaException.class,
                () -> servicio.actualizar(ID_ASOCIACION, solicitud("12000")));

        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_asociacionDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        asociacionEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizar(ID_ASOCIACION, solicitud("12000")));

        verify(repositorio, never()).save(any());
        verifyNoInteractions(proveedorService, productoService);
    }

    // ---- eliminar ----

    @Test
    void eliminar_conAsociacionPropia_laElimina() {
        ProveedorProducto asociacion = asociacionPropia();
        when(repositorio.findByIdAndIdEmpresa(ID_ASOCIACION, ID_EMPRESA)).thenReturn(Optional.of(asociacion));

        servicio.eliminar(ID_ASOCIACION);

        verify(repositorio).delete(asociacion);
    }

    @Test
    void eliminar_asociacionDeOtraEmpresa_seTrataComoNoEncontradoYNoElimina() {
        asociacionEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(ID_ASOCIACION));

        verify(repositorio, never()).delete(any());
    }

    // ---- precio de compra (RN-09) ----

    @Test
    void obtenerPrecioCompra_conAsociacion_devuelveElPrecioDeLaAsociacion() {
        when(repositorio.findByIdEmpresaAndIdProveedorAndIdProducto(ID_EMPRESA, ID_PROVEEDOR, ID_PRODUCTO))
                .thenReturn(Optional.of(asociacionPropia()));

        assertEquals(new BigDecimal("9000"), servicio.obtenerPrecioCompra(ID_PROVEEDOR, ID_PRODUCTO));
    }

    @Test
    void obtenerPrecioCompra_sinAsociacion_seRechaza() {
        when(repositorio.findByIdEmpresaAndIdProveedorAndIdProducto(ID_EMPRESA, ID_PROVEEDOR, ID_PRODUCTO))
                .thenReturn(Optional.empty());

        assertThrows(OperacionInvalidaException.class,
                () -> servicio.obtenerPrecioCompra(ID_PROVEEDOR, ID_PRODUCTO));
    }

    // ---- ayudas ----

    private ProveedorProductoRequest solicitud(String precioCompra) {
        return new ProveedorProductoRequest(ID_PROVEEDOR, ID_PRODUCTO, new BigDecimal(precioCompra));
    }

    private ProveedorProducto asociacionPropia() {
        ProveedorProducto asociacion = new ProveedorProducto();
        asociacion.setIdEmpresa(ID_EMPRESA);
        asociacion.setIdProveedor(ID_PROVEEDOR);
        asociacion.setIdProducto(ID_PRODUCTO);
        asociacion.setPrecioCompra(new BigDecimal("9000"));
        return asociacion;
    }

    private void asociacionEsDeOtraEmpresa() {
        when(repositorio.findByIdAndIdEmpresa(ID_ASOCIACION, ID_EMPRESA)).thenReturn(Optional.empty());
    }
}