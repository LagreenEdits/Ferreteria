package com.xorr.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.SesionDePrueba;
import com.xorr.common.ContextoActual;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProductoRequest;
import com.xorr.inventario.dto.ProductoResponse;
import com.xorr.inventario.entity.Producto;
import com.xorr.inventario.repository.ProductoRepository;
import com.xorr.inventario.repository.ProveedorProductoRepository;

// Cubre crear, listar, obtener y actualizar. El filtro real por empresa vive en las
// consultas del repositorio (Mockito no las ejecuta): aquí se comprueba que el
// servicio las llama con la empresa actual.
@ExtendWith({ MockitoExtension.class, SesionDePrueba.class })
class ProductoServiceCrudTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_PRODUCTO = 10L;

    @Mock
    private ProductoRepository repositorio;
    @Mock
    private ProveedorProductoRepository asociaciones;
    @Mock
    private MovimientosCompra movimientosCompra;
    @Mock
    private MovimientosVenta movimientosVenta;

    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProductoService(repositorio, asociaciones, movimientosCompra,
                movimientosVenta, new ContextoActual());
    }

    @Test
    void crear_guardaElProductoConLaEmpresaActualYLosDatosRecibidos() {
        when(repositorio.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        ProductoRequest datos = new ProductoRequest("Martillo", new BigDecimal("15000"), 10, 2);

        ProductoResponse respuesta = servicio.crear(datos);

        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(repositorio).save(guardado.capture());
        Producto producto = guardado.getValue();
        assertEquals(1L, (long) producto.getIdEmpresa());
        assertEquals("Martillo", producto.getNombre());
        assertEquals(0, new BigDecimal("15000").compareTo(producto.getPrecioVenta()));
        assertEquals(10, (int) producto.getStock());
        assertEquals(2, (int) producto.getStockMinimo());
        assertEquals("Martillo", respuesta.nombre());
    }

    @Test
    void listar_devuelveLosProductosDeLaEmpresaActual() {
        when(repositorio.findByIdEmpresa(ID_EMPRESA))
                .thenReturn(List.of(producto("Martillo"), producto("Clavos")));

        List<ProductoResponse> respuesta = servicio.listar();

        assertEquals(2, respuesta.size());
        assertEquals("Martillo", respuesta.get(0).nombre());
        assertEquals("Clavos", respuesta.get(1).nombre());
    }

    @Test
    void obtener_conProductoDeLaEmpresa_devuelveElProducto() {
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA))
                .thenReturn(Optional.of(producto("Martillo")));

        ProductoResponse respuesta = servicio.obtener(ID_PRODUCTO);

        assertEquals("Martillo", respuesta.nombre());
    }

    // RN-05: un producto de otra empresa se trata como no encontrado.
    @Test
    void obtener_conProductoDeOtraEmpresa_lanzaRecursoNoEncontrado() {
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(ID_PRODUCTO));
    }

    @Test
    void actualizar_copiaLosDatosNuevosYGuarda() {
        Producto existente = producto("Martillo");
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA))
                .thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);
        ProductoRequest datos = new ProductoRequest("Martillo grande", new BigDecimal("20000"), 5, 1);

        ProductoResponse respuesta = servicio.actualizar(ID_PRODUCTO, datos);

        assertEquals("Martillo grande", existente.getNombre());
        assertEquals(0, new BigDecimal("20000").compareTo(existente.getPrecioVenta()));
        assertEquals(5, (int) existente.getStock());
        assertEquals(1, (int) existente.getStockMinimo());
        assertEquals("Martillo grande", respuesta.nombre());
    }

    @Test
    void actualizar_conProductoDeOtraEmpresa_lanzaRecursoNoEncontradoYNoGuarda() {
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA))
                .thenReturn(Optional.empty());
        ProductoRequest datos = new ProductoRequest("Martillo", new BigDecimal("15000"), 10, 2);

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizar(ID_PRODUCTO, datos));
        verify(repositorio, never()).save(any(Producto.class));
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setIdEmpresa(ID_EMPRESA);
        producto.setNombre(nombre);
        producto.setPrecioVenta(new BigDecimal("15000"));
        producto.setStock(10);
        producto.setStockMinimo(2);
        return producto;
    }
}