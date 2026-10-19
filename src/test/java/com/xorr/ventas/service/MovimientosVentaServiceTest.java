package com.xorr.ventas.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xorr.SesionDePrueba;
import com.xorr.common.ContextoActual;
import com.xorr.ventas.repository.FacturaRepository;

// La consulta real (JPQL) la ejecuta la base de datos; aquí solo se comprueba que el
// servicio la usa con la empresa actual. El comportamiento real se verifica a mano
// en Swagger.
@ExtendWith({ MockitoExtension.class, SesionDePrueba.class })
class MovimientosVentaServiceTest {

    private static final Long ID_EMPRESA = 1L;
    private static final Long ID_PRODUCTO = 10L;

    @Mock
    private FacturaRepository facturas;

    private MovimientosVentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new MovimientosVentaService(facturas, new ContextoActual());
    }

    @Test
    void productoTieneFacturas_conFacturasEnLaEmpresaActual_devuelveTrue() {
        when(facturas.existeConProducto(ID_EMPRESA, ID_PRODUCTO)).thenReturn(true);

        assertTrue(servicio.productoTieneFacturas(ID_PRODUCTO));
    }

    @Test
    void productoTieneFacturas_sinFacturas_devuelveFalse() {
        when(facturas.existeConProducto(ID_EMPRESA, ID_PRODUCTO)).thenReturn(false);

        assertFalse(servicio.productoTieneFacturas(ID_PRODUCTO));
    }
}