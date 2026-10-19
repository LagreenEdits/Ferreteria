package com.xorr.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.xorr.common.StockInsuficienteException;
import com.xorr.inventario.dto.ProductoResponse;
import com.xorr.inventario.service.ProductoService;
import com.xorr.ventas.dto.DetalleFacturaRequest;
import com.xorr.ventas.dto.FacturaRequest;
import com.xorr.ventas.dto.FacturaResponse;
import com.xorr.ventas.entity.DetalleFactura;
import com.xorr.ventas.entity.Factura;
import com.xorr.ventas.repository.FacturaRepository;

@ExtendWith({ MockitoExtension.class, SesionDePrueba.class })
class FacturaServiceTest {

    private static final long ID_EMPRESA = 1L;
    private static final long ID_USUARIO = 1L;
    private static final long ID_MARTILLO = 10L;
    private static final long ID_CLAVOS = 11L;
    private static final long ID_FACTURA = 5L;

    @Mock
    private FacturaRepository facturas;

    @Mock
    private ProductoService productoService;

    private FacturaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new FacturaService(facturas, new ContextoActual(), productoService);
    }

    // ---- crear: RN-03 (totales los calcula el servidor) y RN-10 (precio del
    // producto) ----

    @Test
    void crear_conUnaLinea_calculaSubtotalYTotalYDescuentaElStock() {
        existeProducto(ID_MARTILLO, "Martillo", "15000");
        guardarDevuelveLoMismo();

        FacturaResponse respuesta = servicio.crear(solicitud(linea(ID_MARTILLO, 3)));

        assertEquals(0, new BigDecimal("45000").compareTo(respuesta.total()));
        assertEquals(1, respuesta.detalles().size());
        assertEquals(0, new BigDecimal("15000").compareTo(respuesta.detalles().get(0).precioUnitario()));
        assertEquals(0, new BigDecimal("45000").compareTo(respuesta.detalles().get(0).subtotal()));
        verify(productoService).decrementarStock(ID_MARTILLO, 3);
    }

    @Test
    void crear_conVariasLineas_sumaLosSubtotalesYDescuentaCadaProducto() {
        existeProducto(ID_MARTILLO, "Martillo", "15000");
        existeProducto(ID_CLAVOS, "Clavos", "2500");
        guardarDevuelveLoMismo();

        FacturaResponse respuesta = servicio.crear(solicitud(linea(ID_MARTILLO, 2), linea(ID_CLAVOS, 4)));

        // 2 x 15000 + 4 x 2500 = 40000
        assertEquals(0, new BigDecimal("40000").compareTo(respuesta.total()));
        assertEquals(2, respuesta.detalles().size());
        verify(productoService).decrementarStock(ID_MARTILLO, 2);
        verify(productoService).decrementarStock(ID_CLAVOS, 4);
    }

    @Test
    void crear_tomaElPrecioDelProducto_noDelCliente() {
        existeProducto(ID_MARTILLO, "Martillo", "9999.50");
        guardarDevuelveLoMismo();

        FacturaResponse respuesta = servicio.crear(solicitud(linea(ID_MARTILLO, 2)));

        assertEquals(0, new BigDecimal("9999.50").compareTo(respuesta.detalles().get(0).precioUnitario()));
        assertEquals(0, new BigDecimal("19999.00").compareTo(respuesta.total()));
    }

    @Test
    void crear_guardaUnaSolaVezLaFacturaConEmpresaUsuarioYFecha() {
        existeProducto(ID_MARTILLO, "Martillo", "15000");
        guardarDevuelveLoMismo();

        servicio.crear(solicitud(linea(ID_MARTILLO, 1)));

        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturas).save(captor.capture());
        Factura guardada = captor.getValue();
        assertEquals(ID_EMPRESA, guardada.getIdEmpresa());
        assertEquals(ID_USUARIO, guardada.getIdUsuario());
        assertNotNull(guardada.getFecha());
        assertEquals(1, guardada.getDetalles().size());
    }

    // ---- crear: RN-01, RN-06 y RN-05 ----

    @Test
    void crear_conStockInsuficiente_lanzaExcepcionYNoGuardaLaFactura() {
        existeProducto(ID_MARTILLO, "Martillo", "15000");
        doThrow(new StockInsuficienteException("Stock insuficiente para 'Martillo'"))
                .when(productoService).decrementarStock(ID_MARTILLO, 5);

        assertThrows(StockInsuficienteException.class,
                () -> servicio.crear(solicitud(linea(ID_MARTILLO, 5))));

        verify(facturas, never()).save(any());
    }

    // RN-06: si una línea falla, no se guarda nada. El "deshacer" real de la
    // transacción lo hace Spring con @Transactional; aquí solo se comprueba que la
    // factura nunca llega a guardarse.
    @Test
    void crear_siFallaLaSegundaLinea_noGuardaLaFactura() {
        existeProducto(ID_MARTILLO, "Martillo", "15000");
        existeProducto(ID_CLAVOS, "Clavos", "2500");
        doNothing().when(productoService).decrementarStock(ID_MARTILLO, 1);
        doThrow(new StockInsuficienteException("Stock insuficiente para 'Clavos'"))
                .when(productoService).decrementarStock(ID_CLAVOS, 100);

        assertThrows(StockInsuficienteException.class,
                () -> servicio.crear(solicitud(linea(ID_MARTILLO, 1), linea(ID_CLAVOS, 100))));

        verify(productoService).decrementarStock(ID_MARTILLO, 1);
        verify(facturas, never()).save(any());
    }

    @Test
    void crear_conProductoDeOtraEmpresa_seTrataComoNoEncontradoYNoDescuentaStock() {
        when(productoService.obtener(ID_MARTILLO))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado: " + ID_MARTILLO));

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(solicitud(linea(ID_MARTILLO, 1))));

        verify(productoService, never()).decrementarStock(anyLong(), anyInt());
        verify(facturas, never()).save(any());
    }

    // ---- crear: RN-15 (ciclo TDD 16.5) ----

    // RN-15: el mismo producto en dos líneas se rechaza antes de tocar el stock.
    // lenient: con la regla implementada el producto ni se consulta, pero sin ella
    // el stub evita que la prueba falle por un null y no por la regla ausente.
    @Test
    void crear_conElMismoProductoEnDosLineas_seRechazaYNoTocaElStock() {
        lenient().when(productoService.obtener(ID_MARTILLO))
                .thenReturn(new ProductoResponse(ID_MARTILLO, "Martillo", new BigDecimal("15000"), 100, 5));

        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                () -> servicio.crear(solicitud(linea(ID_MARTILLO, 1), linea(ID_MARTILLO, 2))));

        assertEquals("La factura repite un producto en varias líneas; usa una sola línea con la cantidad total",
                error.getMessage());
        verify(productoService, never()).decrementarStock(anyLong(), anyInt());
        verify(facturas, never()).save(any());
    }

    // ---- obtener y listar (RN-05) ----

    @Test
    void obtener_facturaPropia_devuelveSusDatos() {
        when(facturas.findByIdAndIdEmpresa(ID_FACTURA, ID_EMPRESA))
                .thenReturn(Optional.of(facturaConUnaLinea()));

        FacturaResponse respuesta = servicio.obtener(ID_FACTURA);

        assertEquals(ID_USUARIO, respuesta.idUsuario());
        assertEquals(0, new BigDecimal("30000").compareTo(respuesta.total()));
        assertEquals(1, respuesta.detalles().size());
    }

    @Test
    void obtener_facturaDeOtraEmpresa_seTrataComoNoEncontrado() {
        when(facturas.findByIdAndIdEmpresa(ID_FACTURA, ID_EMPRESA)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtener(ID_FACTURA));

        assertEquals("Factura no encontrada: " + ID_FACTURA, error.getMessage());
    }

    @Test
    void listar_devuelveLasFacturasDeLaEmpresaActual() {
        when(facturas.findByIdEmpresa(ID_EMPRESA)).thenReturn(List.of(facturaConUnaLinea()));

        List<FacturaResponse> resultado = servicio.listar();

        assertEquals(1, resultado.size());
    }

    @Test
    void listar_sinFacturas_devuelveListaVacia() {
        when(facturas.findByIdEmpresa(ID_EMPRESA)).thenReturn(List.of());

        assertTrue(servicio.listar().isEmpty());
    }

    // ---- ayudas ----

    private FacturaRequest solicitud(DetalleFacturaRequest... lineas) {
        return new FacturaRequest(List.of(lineas));
    }

    private DetalleFacturaRequest linea(long idProducto, int cantidad) {
        return new DetalleFacturaRequest(idProducto, cantidad);
    }

    private void existeProducto(long idProducto, String nombre, String precioVenta) {
        when(productoService.obtener(idProducto))
                .thenReturn(new ProductoResponse(idProducto, nombre, new BigDecimal(precioVenta), 100, 5));
    }

    private void guardarDevuelveLoMismo() {
        when(facturas.save(any(Factura.class))).thenAnswer(llamada -> llamada.getArgument(0));
    }

    private Factura facturaConUnaLinea() {
        DetalleFactura detalle = new DetalleFactura();
        detalle.setIdProducto(ID_MARTILLO);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("15000"));
        detalle.setSubtotal(new BigDecimal("30000"));

        Factura factura = new Factura();
        factura.setIdEmpresa(ID_EMPRESA);
        factura.setIdUsuario(ID_USUARIO);
        factura.setFecha(LocalDateTime.now());
        factura.setTotal(new BigDecimal("30000"));
        factura.getDetalles().add(detalle);
        return factura;
    }
}