package com.xorr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.xorr.common.ContextoActual;
import com.xorr.common.DatosToken;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.common.Rol;
import com.xorr.common.StockInsuficienteException;
import com.xorr.compras.entity.DetalleOrdenCompra;
import com.xorr.compras.entity.EstadoOrdenCompra;
import com.xorr.compras.entity.OrdenCompra;
import com.xorr.compras.repository.OrdenCompraRepository;
import com.xorr.compras.service.OrdenCompraService;
import com.xorr.inventario.entity.Producto;
import com.xorr.inventario.repository.ProductoRepository;
import com.xorr.ventas.dto.DetalleFacturaRequest;
import com.xorr.ventas.dto.FacturaRequest;
import com.xorr.ventas.repository.FacturaRepository;
import com.xorr.ventas.service.FacturaService;

// RN-06 con la aplicación completa y H2: comprueba el rollback real de las
// transacciones, que las pruebas con Mockito no pueden probar.
// IMPORTANTE: esta clase NO lleva @Transactional. Si la llevara, el servicio
// se uniría a la transacción de la prueba y el rollback real no se vería.
// Como no pasa por HTTP, simula la sesión que normalmente crea el filtro JWT.
@SpringBootTest
class RollbackIntegracionTest {

    private static final long PRODUCTO_INEXISTENTE = 999_999L;

    @Autowired
    private FacturaService facturaService;
    @Autowired
    private OrdenCompraService ordenCompraService;
    @Autowired
    private ProductoRepository productos;
    @Autowired
    private FacturaRepository facturas;
    @Autowired
    private OrdenCompraRepository ordenes;
    @Autowired
    private ContextoActual contexto;

    @BeforeEach
    void iniciarSesion() {
        DatosToken datos = new DatosToken(1L, 1L, Rol.ADMIN);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(datos, null, List.of()));
    }

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    @AfterEach
    void limpiar() {
        facturas.deleteAll();
        ordenes.deleteAll();
        productos.deleteAll();
    }

    // Control: sin fallos, la venta se guarda y descuenta. Demuestra que la
    // configuración de la prueba funciona y que el rollback de abajo es real.
    @Test
    void crearFactura_conStockSuficiente_descuentaElStockYGuardaLaFactura() {
        Producto a = guardarProducto("A", 10);

        facturaService.crear(new FacturaRequest(List.of(new DetalleFacturaRequest(a.getId(), 3))));

        assertEquals(7, stockDe(a));
        assertEquals(1, facturas.count());
    }

    @Test
    void crearFactura_siFallaLaSegundaLinea_revierteElStockDeLaPrimeraYNoGuardaNada() {
        Producto a = guardarProducto("A", 10);
        Producto b = guardarProducto("B", 1);
        FacturaRequest pedido = new FacturaRequest(List.of(
                new DetalleFacturaRequest(a.getId(), 3),
                new DetalleFacturaRequest(b.getId(), 5)));

        assertThrows(StockInsuficienteException.class, () -> facturaService.crear(pedido));

        assertEquals(10, stockDe(a));
        assertEquals(1, stockDe(b));
        assertEquals(0, facturas.count());
    }

    @Test
    void recibirOrden_siFallaLaSegundaLinea_revierteElStockDeLaPrimeraYLaOrdenSigueSiendoPendiente() {
        Producto a = guardarProducto("A", 10);
        OrdenCompra orden = ordenes.save(ordenPendiente(
                linea(a.getId(), 4),
                linea(PRODUCTO_INEXISTENTE, 2)));

        assertThrows(RecursoNoEncontradoException.class, () -> ordenCompraService.recibir(orden.getId()));

        assertEquals(10, stockDe(a));
        assertEquals(EstadoOrdenCompra.PENDIENTE, ordenes.findById(orden.getId()).orElseThrow().getEstado());
    }

    private Producto guardarProducto(String nombre, int stock) {
        Producto producto = new Producto();
        producto.setIdEmpresa(contexto.getIdEmpresa());
        producto.setNombre(nombre);
        producto.setPrecioVenta(BigDecimal.valueOf(1000));
        producto.setStock(stock);
        producto.setStockMinimo(0);
        return productos.save(producto);
    }

    private int stockDe(Producto producto) {
        return productos.findById(producto.getId()).orElseThrow().getStock();
    }

    private DetalleOrdenCompra linea(Long idProducto, int cantidad) {
        DetalleOrdenCompra detalle = new DetalleOrdenCompra();
        detalle.setIdProducto(idProducto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(BigDecimal.TEN);
        detalle.setSubtotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(cantidad)));
        return detalle;
    }

    private OrdenCompra ordenPendiente(DetalleOrdenCompra... lineas) {
        OrdenCompra orden = new OrdenCompra();
        orden.setIdEmpresa(contexto.getIdEmpresa());
        orden.setIdProveedor(1L);
        orden.setIdUsuario(contexto.getIdUsuario());
        orden.setFecha(LocalDateTime.now());
        orden.setEstado(EstadoOrdenCompra.PENDIENTE);
        orden.setTotal(BigDecimal.ZERO);
        orden.getDetalles().addAll(List.of(lineas));
        return orden;
    }
}