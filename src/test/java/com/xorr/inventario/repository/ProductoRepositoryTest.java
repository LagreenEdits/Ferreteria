package com.xorr.inventario.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.xorr.inventario.entity.Producto;

// Prueba de integración con H2: ejecuta las consultas reales del repositorio,
// que las pruebas con Mockito no ejecutan. Cierra el hueco de RN-04 (el "<"
// estricto) y comprueba el filtro por empresa de RN-05.
@DataJpaTest
class ProductoRepositoryTest {

    private static final long EMPRESA = 1L;
    private static final long OTRA_EMPRESA = 2L;

    @Autowired
    private ProductoRepository productos;

    @Test
    void buscarConStockBajo_conStockMenorAlMinimo_apareceEnLaLista() {
        guardar(EMPRESA, "Martillo", 2, 5);

        assertEquals(List.of("Martillo"), nombres(productos.buscarConStockBajo(EMPRESA)));
    }

    @Test
    void buscarConStockBajo_conStockIgualOMayorAlMinimo_noAparece() {
        guardar(EMPRESA, "Igual", 5, 5);
        guardar(EMPRESA, "Mayor", 6, 5);
        guardar(EMPRESA, "CeroIgualACero", 0, 0);

        assertTrue(productos.buscarConStockBajo(EMPRESA).isEmpty());
    }

    @Test
    void buscarConStockBajo_soloDevuelveLosProductosDeLaEmpresaPedida() {
        guardar(EMPRESA, "Propio", 1, 5);
        guardar(OTRA_EMPRESA, "Ajeno", 1, 5);

        assertEquals(List.of("Propio"), nombres(productos.buscarConStockBajo(EMPRESA)));
    }

    @Test
    void findByIdAndIdEmpresa_conProductoDeOtraEmpresa_noDevuelveNada() {
        Producto ajeno = guardar(OTRA_EMPRESA, "Ajeno", 1, 5);

        assertTrue(productos.findByIdAndIdEmpresa(ajeno.getId(), EMPRESA).isEmpty());
        assertTrue(productos.findByIdAndIdEmpresa(ajeno.getId(), OTRA_EMPRESA).isPresent());
    }

    private Producto guardar(long idEmpresa, String nombre, int stock, int stockMinimo) {
        Producto producto = new Producto();
        producto.setIdEmpresa(idEmpresa);
        producto.setNombre(nombre);
        producto.setPrecioVenta(BigDecimal.valueOf(1000));
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        return productos.saveAndFlush(producto);
    }

    private List<String> nombres(List<Producto> lista) {
        return lista.stream().map(Producto::getNombre).toList();
    }
}