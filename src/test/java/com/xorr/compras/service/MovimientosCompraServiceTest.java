package com.xorr.compras.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.common.ContextoActual;
import com.xorr.compras.repository.OrdenCompraRepository;

// Las consultas reales (JPQL y derivada) las ejecuta la base de datos; aquí solo se
// comprueba que el servicio las usa con la empresa actual. El comportamiento real
// se verifica a mano en Swagger.
@ExtendWith(MockitoExtension.class)
class MovimientosCompraServiceTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_PRODUCTO = 10L;
    private static final Long ID_PROVEEDOR = 20L;

    @Mock
    private OrdenCompraRepository ordenes;

    private MovimientosCompraService servicio;

    @BeforeEach
    void preparar() {
        servicio = new MovimientosCompraService(ordenes, new ContextoActual());
    }

    @Test
    void productoTieneOrdenes_conOrdenesEnLaEmpresaActual_devuelveTrue() {
        when(ordenes.existeConProducto(ID_EMPRESA, ID_PRODUCTO)).thenReturn(true);

        assertTrue(servicio.productoTieneOrdenes(ID_PRODUCTO));
    }

    @Test
    void productoTieneOrdenes_sinOrdenes_devuelveFalse() {
        when(ordenes.existeConProducto(ID_EMPRESA, ID_PRODUCTO)).thenReturn(false);

        assertFalse(servicio.productoTieneOrdenes(ID_PRODUCTO));
    }

    @Test
    void proveedorTieneOrdenes_conOrdenesEnLaEmpresaActual_devuelveTrue() {
        when(ordenes.existsByIdEmpresaAndIdProveedor(ID_EMPRESA, ID_PROVEEDOR)).thenReturn(true);

        assertTrue(servicio.proveedorTieneOrdenes(ID_PROVEEDOR));
    }

    @Test
    void proveedorTieneOrdenes_sinOrdenes_devuelveFalse() {
        when(ordenes.existsByIdEmpresaAndIdProveedor(ID_EMPRESA, ID_PROVEEDOR)).thenReturn(false);

        assertFalse(servicio.proveedorTieneOrdenes(ID_PROVEEDOR));
    }
}