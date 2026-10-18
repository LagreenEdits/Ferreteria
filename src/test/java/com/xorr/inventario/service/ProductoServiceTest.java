package com.xorr.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.common.StockInsuficienteException;
import com.xorr.compras.service.MovimientosCompraService;
import com.xorr.inventario.dto.ProductoResponse;
import com.xorr.inventario.entity.Producto;
import com.xorr.inventario.repository.ProductoRepository;
import com.xorr.inventario.repository.ProveedorProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final long ID_EMPRESA = 1L;
    private static final long ID_PRODUCTO = 10L;

    @Mock
    private ProductoRepository repositorio;

    @Mock
    private ProveedorProductoRepository asociaciones;

    @Mock
    private MovimientosCompraService movimientosCompra;

    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProductoService(repositorio, asociaciones, movimientosCompra, new ContextoActual());
    }

    // ---- decrementarStock (RN-01, RN-06, RN-07) ----

    @Test
    void decrementar_conStockSuficiente_descuentaLaCantidadCorrecta() {
        Producto producto = productoConStock(10, 2);
        existeProductoPropio(producto);

        servicio.decrementarStock(ID_PRODUCTO, 3);

        assertEquals(7, producto.getStock());
        verify(repositorio).save(producto);
    }

    @Test
    void decrementar_conStockInsuficiente_lanzaExcepcionYNoCambiaElStock() {
        Producto producto = productoConStock(2, 1);
        existeProductoPropio(producto);

        assertThrows(StockInsuficienteException.class,
                () -> servicio.decrementarStock(ID_PRODUCTO, 5));

        assertEquals(2, producto.getStock());
        verify(repositorio, never()).save(any());
    }

    @Test
    void decrementar_conStockExactamenteIgualALaCantidad_funcionaYDejaCero() {
        Producto producto = productoConStock(4, 1);
        existeProductoPropio(producto);

        servicio.decrementarStock(ID_PRODUCTO, 4);

        assertEquals(0, producto.getStock());
        verify(repositorio).save(producto);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -10 })
    void decrementar_conCantidadCeroONegativa_seRechaza(int cantidad) {
        assertThrows(OperacionInvalidaException.class,
                () -> servicio.decrementarStock(ID_PRODUCTO, cantidad));

        verifyNoInteractions(repositorio);
    }

    // ---- incrementarStock (RN-07) ----

    @Test
    void incrementar_sumaLaCantidadAlStock() {
        Producto producto = productoConStock(5, 2);
        existeProductoPropio(producto);

        servicio.incrementarStock(ID_PRODUCTO, 8);

        assertEquals(13, producto.getStock());
        verify(repositorio).save(producto);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -10 })
    void incrementar_conCantidadCeroONegativa_seRechaza(int cantidad) {
        assertThrows(OperacionInvalidaException.class,
                () -> servicio.incrementarStock(ID_PRODUCTO, cantidad));

        verifyNoInteractions(repositorio);
    }

    // ---- stock bajo (RN-04) ----
    // El "<" estricto vive en la consulta JPQL del repositorio; aquí solo se
    // comprueba que el
    // servicio la usa con la empresa actual y devuelve DTOs.

    @Test
    void listarStockBajo_devuelveLosProductosQueElRepositorioReportaBajoElMinimo() {
        when(repositorio.buscarConStockBajo(ID_EMPRESA)).thenReturn(List.of(productoConStock(2, 5)));

        List<ProductoResponse> resultado = servicio.listarStockBajo();

        assertEquals(1, resultado.size());
        assertEquals("Martillo", resultado.get(0).nombre());
    }

    @Test
    void listarStockBajo_sinProductosBajoElMinimo_devuelveListaVacia() {
        when(repositorio.buscarConStockBajo(ID_EMPRESA)).thenReturn(List.of());

        assertTrue(servicio.listarStockBajo().isEmpty());
    }

    // ---- eliminar (RN-12 y RN-14) ----

    @Test
    void eliminar_conProductoSinOrdenes_loEliminaJuntoConSusAsociaciones() {
        Producto producto = productoConStock(5, 2);
        existeProductoPropio(producto);
        when(movimientosCompra.productoTieneOrdenes(ID_PRODUCTO)).thenReturn(false);

        servicio.eliminar(ID_PRODUCTO);

        verify(asociaciones).deleteByIdEmpresaAndIdProducto(ID_EMPRESA, ID_PRODUCTO);
        verify(repositorio).delete(producto);
    }

    @Test
    void eliminar_conProductoConOrdenesDeCompra_seRechazaYNoBorraNada() {
        existeProductoPropio(productoConStock(5, 2));
        when(movimientosCompra.productoTieneOrdenes(ID_PRODUCTO)).thenReturn(true);

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> servicio.eliminar(ID_PRODUCTO));

        assertEquals("No se puede eliminar el producto 'Martillo' porque tiene órdenes de compra",
                error.getMessage());
        verify(repositorio, never()).delete(any());
        verifyNoInteractions(asociaciones);
    }

    @Test
    void eliminar_productoDeOtraEmpresa_seTrataComoNoEncontradoYNoBorraNada() {
        productoEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(ID_PRODUCTO));

        verify(repositorio, never()).delete(any());
        verifyNoInteractions(asociaciones, movimientosCompra);
    }

    // ---- multi-tenant (RN-05) ----

    @Test
    void obtener_productoDeOtraEmpresa_seTrataComoNoEncontrado() {
        productoEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(ID_PRODUCTO));
    }

    @Test
    void decrementar_productoDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        productoEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.decrementarStock(ID_PRODUCTO, 1));

        verify(repositorio, never()).save(any());
    }

    @Test
    void incrementar_productoDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        productoEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.incrementarStock(ID_PRODUCTO, 1));

        verify(repositorio, never()).save(any());
    }

    // ---- ayudas ----

    private Producto productoConStock(int stock, int stockMinimo) {
        Producto producto = new Producto();
        producto.setIdEmpresa(ID_EMPRESA);
        producto.setNombre("Martillo");
        producto.setPrecioVenta(new BigDecimal("15000"));
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        return producto;
    }

    private void existeProductoPropio(Producto producto) {
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA)).thenReturn(Optional.of(producto));
    }

    private void productoEsDeOtraEmpresa() {
        when(repositorio.findByIdAndIdEmpresa(ID_PRODUCTO, ID_EMPRESA)).thenReturn(Optional.empty());
    }
}