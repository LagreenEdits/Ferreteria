package com.xorr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.xorr.compras.dto.DetalleOrdenCompraRequest;
import com.xorr.compras.dto.OrdenCompraRequest;
import com.xorr.inventario.dto.ProductoRequest;
import com.xorr.inventario.dto.ProveedorProductoRequest;
import com.xorr.ventas.dto.DetalleFacturaRequest;
import com.xorr.ventas.dto.FacturaRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

// RN-07 en los DTO: Mockito no ejecuta Bean Validation, así que aquí se usa el
// Validator real, sin Spring ni base de datos. Se comprueba qué campos fallan.
class ValidacionDtoTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> camposInvalidos(Object dto) {
        return validator.validate(dto).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    // ---------- ProductoRequest ----------

    @Test
    void productoRequest_conLimitesValidos_noTieneViolaciones() {
        assertEquals(Set.of(), camposInvalidos(new ProductoRequest("Martillo", BigDecimal.ZERO, 0, 0)));
    }

    @Test
    void productoRequest_conStockNegativo_marcaElCampoStock() {
        assertEquals(Set.of("stock"), camposInvalidos(new ProductoRequest("Martillo", BigDecimal.TEN, -1, 5)));
    }

    @Test
    void productoRequest_conPrecioVentaNegativo_marcaElCampoPrecioVenta() {
        assertEquals(Set.of("precioVenta"),
                camposInvalidos(new ProductoRequest("Martillo", new BigDecimal("-0.01"), 1, 5)));
    }

    @Test
    void productoRequest_conStockMinimoNegativo_marcaElCampoStockMinimo() {
        assertEquals(Set.of("stockMinimo"), camposInvalidos(new ProductoRequest("Martillo", BigDecimal.TEN, 1, -1)));
    }

    @Test
    void productoRequest_conCamposNulosOVacios_marcaTodosLosCampos() {
        assertEquals(Set.of("nombre", "precioVenta", "stock", "stockMinimo"),
                camposInvalidos(new ProductoRequest(" ", null, null, null)));
    }

    // ---------- ProveedorProductoRequest ----------

    @Test
    void proveedorProductoRequest_conPrecioCeroYIds_noTieneViolaciones() {
        assertEquals(Set.of(), camposInvalidos(new ProveedorProductoRequest(1L, 1L, BigDecimal.ZERO)));
    }

    @Test
    void proveedorProductoRequest_conPrecioCompraNegativo_marcaElCampoPrecioCompra() {
        assertEquals(Set.of("precioCompra"),
                camposInvalidos(new ProveedorProductoRequest(1L, 1L, new BigDecimal("-1"))));
    }

    @Test
    void proveedorProductoRequest_conCamposNulos_marcaTodosLosCampos() {
        assertEquals(Set.of("idProveedor", "idProducto", "precioCompra"),
                camposInvalidos(new ProveedorProductoRequest(null, null, null)));
    }

    // ---------- DetalleOrdenCompraRequest ----------

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -100 })
    void detalleOrdenCompraRequest_conCantidadNoPositiva_marcaElCampoCantidad(int cantidad) {
        assertEquals(Set.of("cantidad"), camposInvalidos(new DetalleOrdenCompraRequest(1L, cantidad)));
    }

    @Test
    void detalleOrdenCompraRequest_conCamposNulos_marcaTodosLosCampos() {
        assertEquals(Set.of("idProducto", "cantidad"), camposInvalidos(new DetalleOrdenCompraRequest(null, null)));
    }

    @Test
    void detalleOrdenCompraRequest_conCantidadUno_noTieneViolaciones() {
        assertEquals(Set.of(), camposInvalidos(new DetalleOrdenCompraRequest(1L, 1)));
    }

    // ---------- DetalleFacturaRequest ----------

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -100 })
    void detalleFacturaRequest_conCantidadNoPositiva_marcaElCampoCantidad(int cantidad) {
        assertEquals(Set.of("cantidad"), camposInvalidos(new DetalleFacturaRequest(1L, cantidad)));
    }

    @Test
    void detalleFacturaRequest_conCamposNulos_marcaTodosLosCampos() {
        assertEquals(Set.of("idProducto", "cantidad"), camposInvalidos(new DetalleFacturaRequest(null, null)));
    }

    @Test
    void detalleFacturaRequest_conCantidadUno_noTieneViolaciones() {
        assertEquals(Set.of(), camposInvalidos(new DetalleFacturaRequest(1L, 1)));
    }

    // ---------- Listas de detalles (@Valid y @NotEmpty) ----------

    @Test
    void facturaRequest_conLineaDeCantidadNegativa_marcaLaLineaDeLaLista() {
        FacturaRequest pedido = new FacturaRequest(List.of(new DetalleFacturaRequest(1L, -1)));

        assertEquals(Set.of("detalles[0].cantidad"), camposInvalidos(pedido));
    }

    @Test
    void facturaRequest_conListaVacia_marcaElCampoDetalles() {
        assertEquals(Set.of("detalles"), camposInvalidos(new FacturaRequest(List.of())));
    }

    @Test
    void ordenCompraRequest_conLineaDeCantidadNegativa_marcaLaLineaDeLaLista() {
        OrdenCompraRequest pedido = new OrdenCompraRequest(1L, List.of(new DetalleOrdenCompraRequest(1L, -1)));

        assertEquals(Set.of("detalles[0].cantidad"), camposInvalidos(pedido));
    }

    @Test
    void ordenCompraRequest_conListaVacia_marcaElCampoDetalles() {
        assertEquals(Set.of("detalles"), camposInvalidos(new OrdenCompraRequest(1L, List.of())));
    }
}