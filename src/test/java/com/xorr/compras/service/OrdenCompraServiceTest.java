package com.xorr.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.compras.dto.DetalleOrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraResponse;
import com.xorr.compras.entity.DetalleOrdenCompra;
import com.xorr.compras.entity.EstadoOrdenCompra;
import com.xorr.compras.entity.OrdenCompra;
import com.xorr.compras.repository.OrdenCompraRepository;
import com.xorr.inventario.service.ProductoService;
import com.xorr.inventario.service.ProveedorProductoService;
import com.xorr.inventario.service.ProveedorService;

@ExtendWith({ MockitoExtension.class, SesionDePrueba.class })
class OrdenCompraServiceTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_USUARIO = 1L;
    private static final Long ID_ORDEN = 40L;
    private static final Long ID_PROVEEDOR = 20L;
    private static final Long ID_PRODUCTO = 10L;
    private static final Long OTRO_PRODUCTO = 11L;

    @Mock
    private OrdenCompraRepository ordenes;

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private ProductoService productoService;

    @Mock
    private ProveedorProductoService proveedorProductoService;

    private OrdenCompraService servicio;

    @BeforeEach
    void preparar() {
        servicio = new OrdenCompraService(
                ordenes, new ContextoActual(), proveedorService, productoService, proveedorProductoService);
    }

    // ---- crear (RN-03, RN-05, RN-09) ----

    @Test
    void crear_calculaPreciosSubtotalesYTotalEnElServidor() {
        when(proveedorProductoService.obtenerPrecioCompra(ID_PROVEEDOR, ID_PRODUCTO))
                .thenReturn(new BigDecimal("9000"));
        when(proveedorProductoService.obtenerPrecioCompra(ID_PROVEEDOR, OTRO_PRODUCTO))
                .thenReturn(new BigDecimal("2500"));
        when(ordenes.save(any(OrdenCompra.class))).thenAnswer(llamada -> llamada.getArgument(0));

        servicio.crear(new OrdenCompraRequest(ID_PROVEEDOR, List.of(
                new DetalleOrdenCompraRequest(ID_PRODUCTO, 3),
                new DetalleOrdenCompraRequest(OTRO_PRODUCTO, 2))));

        ArgumentCaptor<OrdenCompra> guardada = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenes).save(guardada.capture());
        OrdenCompra orden = guardada.getValue();
        assertEquals(EstadoOrdenCompra.PENDIENTE, orden.getEstado());
        assertEquals(ID_EMPRESA, orden.getIdEmpresa());
        assertEquals(ID_USUARIO, orden.getIdUsuario());
        assertEquals(2, orden.getDetalles().size());
        assertDinero("9000", orden.getDetalles().get(0).getPrecioUnitario());
        assertDinero("27000", orden.getDetalles().get(0).getSubtotal());
        assertDinero("5000", orden.getDetalles().get(1).getSubtotal());
        assertDinero("32000", orden.getTotal());
        verify(productoService).obtener(ID_PRODUCTO);
        verify(productoService).obtener(OTRO_PRODUCTO);
    }

    @Test
    void crear_conProductoNoAsociadoAlProveedor_seRechazaYNoGuarda() {
        when(proveedorProductoService.obtenerPrecioCompra(ID_PROVEEDOR, ID_PRODUCTO))
                .thenThrow(new OperacionInvalidaException("El producto no está asociado al proveedor"));

        assertThrows(OperacionInvalidaException.class, () -> servicio.crear(solicitudDeUnaLinea()));

        verify(ordenes, never()).save(any());
    }

    @Test
    void crear_conProveedorDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        when(proveedorService.obtener(ID_PROVEEDOR))
                .thenThrow(new RecursoNoEncontradoException("Proveedor no encontrado"));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(solicitudDeUnaLinea()));

        verify(ordenes, never()).save(any());
        verifyNoInteractions(productoService, proveedorProductoService);
    }

    @Test
    void crear_conProductoDeOtraEmpresa_seTrataComoNoEncontradoYNoGuarda() {
        when(productoService.obtener(ID_PRODUCTO))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(solicitudDeUnaLinea()));

        verify(ordenes, never()).save(any());
    }

    // RN-16 (M2, ROJO): una orden no puede repetir un producto en dos líneas.
    @Test
    void crear_conElMismoProductoEnDosLineas_seRechazaYNoGuarda() {
        OrdenCompraRequest peticion = new OrdenCompraRequest(ID_PROVEEDOR, List.of(
                new DetalleOrdenCompraRequest(ID_PRODUCTO, 2),
                new DetalleOrdenCompraRequest(ID_PRODUCTO, 3)));

        assertThrows(OperacionInvalidaException.class, () -> servicio.crear(peticion));

        verify(ordenes, never()).save(any());
        verifyNoInteractions(proveedorService);
    }

    // ---- recibir (RN-02, RN-06, RN-08) ----

    @Test
    void recibir_ordenPendiente_incrementaElStockDeCadaLineaYQuedaRecibida() {
        OrdenCompra orden = ordenConEstado(EstadoOrdenCompra.PENDIENTE);
        existeOrden(orden);
        when(ordenes.save(orden)).thenReturn(orden);

        servicio.recibir(ID_ORDEN);

        verify(productoService).incrementarStock(ID_PRODUCTO, 3);
        verify(productoService).incrementarStock(OTRO_PRODUCTO, 2);
        assertEquals(EstadoOrdenCompra.RECIBIDA, orden.getEstado());
    }

    @Test
    void recibir_ordenYaRecibida_seRechazaYNoTocaElStock() {
        existeOrden(ordenConEstado(EstadoOrdenCompra.RECIBIDA));

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> servicio.recibir(ID_ORDEN));

        assertEquals("No se puede recibir una orden en estado RECIBIDA", error.getMessage());
        verifyNoInteractions(productoService);
        verify(ordenes, never()).save(any());
    }

    @Test
    void recibir_ordenCancelada_seRechazaYNoTocaElStock() {
        existeOrden(ordenConEstado(EstadoOrdenCompra.CANCELADA));

        assertThrows(OperacionInvalidaException.class, () -> servicio.recibir(ID_ORDEN));

        verifyNoInteractions(productoService);
        verify(ordenes, never()).save(any());
    }

    @Test
    void recibir_siFallaUnaLinea_elErrorSePropagaYLaOrdenNoQuedaRecibida() {
        OrdenCompra orden = ordenConEstado(EstadoOrdenCompra.PENDIENTE);
        existeOrden(orden);
        // La primera línea (producto 10) se procesa bien; la segunda (producto 11)
        // falla.
        doNothing().when(productoService).incrementarStock(ID_PRODUCTO, 3);
        doThrow(new RecursoNoEncontradoException("Producto no encontrado: 11"))
                .when(productoService).incrementarStock(OTRO_PRODUCTO, 2);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.recibir(ID_ORDEN));

        assertEquals(EstadoOrdenCompra.PENDIENTE, orden.getEstado());
        verify(ordenes, never()).save(any());
    }

    @Test
    void recibir_ordenDeOtraEmpresa_seTrataComoNoEncontradaYNoTocaElStock() {
        ordenEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.recibir(ID_ORDEN));

        verifyNoInteractions(productoService);
    }

    // ---- cancelar (RN-08) ----

    @Test
    void cancelar_ordenPendiente_quedaCanceladaSinTocarElStock() {
        OrdenCompra orden = ordenConEstado(EstadoOrdenCompra.PENDIENTE);
        existeOrden(orden);
        when(ordenes.save(orden)).thenReturn(orden);

        servicio.cancelar(ID_ORDEN);

        assertEquals(EstadoOrdenCompra.CANCELADA, orden.getEstado());
        verifyNoInteractions(productoService);
    }

    @Test
    void cancelar_ordenRecibida_seRechaza() {
        existeOrden(ordenConEstado(EstadoOrdenCompra.RECIBIDA));

        assertThrows(OperacionInvalidaException.class, () -> servicio.cancelar(ID_ORDEN));

        verify(ordenes, never()).save(any());
    }

    @Test
    void cancelar_ordenDeOtraEmpresa_seTrataComoNoEncontrada() {
        ordenEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cancelar(ID_ORDEN));

        verify(ordenes, never()).save(any());
    }

    // ---- consultas (RN-05) ----

    @Test
    void obtener_ordenDeOtraEmpresa_seTrataComoNoEncontrada() {
        ordenEsDeOtraEmpresa();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(ID_ORDEN));
    }

    @Test
    void listar_consultaSoloLaEmpresaActual() {
        when(ordenes.findByIdEmpresa(ID_EMPRESA)).thenReturn(List.of(ordenConEstado(EstadoOrdenCompra.PENDIENTE)));

        List<OrdenCompraResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
        assertEquals(EstadoOrdenCompra.PENDIENTE, resultado.get(0).estado());
    }

    // ---- ayudas ----

    private OrdenCompraRequest solicitudDeUnaLinea() {
        return new OrdenCompraRequest(ID_PROVEEDOR, List.of(new DetalleOrdenCompraRequest(ID_PRODUCTO, 3)));
    }

    private OrdenCompra ordenConEstado(EstadoOrdenCompra estado) {
        OrdenCompra orden = new OrdenCompra();
        orden.setIdEmpresa(ID_EMPRESA);
        orden.setIdProveedor(ID_PROVEEDOR);
        orden.setIdUsuario(ID_USUARIO);
        orden.setFecha(LocalDateTime.of(2026, 10, 18, 9, 0));
        orden.setEstado(estado);
        orden.getDetalles().add(detalle(ID_PRODUCTO, 3, "9000"));
        orden.getDetalles().add(detalle(OTRO_PRODUCTO, 2, "2500"));
        orden.setTotal(new BigDecimal("32000"));
        return orden;
    }

    private DetalleOrdenCompra detalle(Long idProducto, int cantidad, String precio) {
        DetalleOrdenCompra detalle = new DetalleOrdenCompra();
        detalle.setIdProducto(idProducto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        detalle.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        return detalle;
    }

    private void existeOrden(OrdenCompra orden) {
        when(ordenes.findByIdAndIdEmpresa(ID_ORDEN, ID_EMPRESA)).thenReturn(Optional.of(orden));
    }

    private void ordenEsDeOtraEmpresa() {
        when(ordenes.findByIdAndIdEmpresa(ID_ORDEN, ID_EMPRESA)).thenReturn(Optional.empty());
    }

    // compareTo evita que 27000 y 27000.00 se consideren distintos por la escala.
    private static void assertDinero(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real));
    }
}