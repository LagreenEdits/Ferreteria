package com.xorr.inventario.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.common.StockInsuficienteException;
import com.xorr.compras.service.MovimientosCompraService;
import com.xorr.inventario.dto.ProductoRequest;
import com.xorr.inventario.dto.ProductoResponse;
import com.xorr.inventario.entity.Producto;
import com.xorr.inventario.repository.ProductoRepository;
import com.xorr.inventario.repository.ProveedorProductoRepository;
import com.xorr.ventas.service.MovimientosVentaService;

@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final ProveedorProductoRepository asociaciones;
    private final MovimientosCompraService movimientosCompra;
    private final MovimientosVentaService movimientosVenta;
    private final ContextoActual contexto;

    public ProductoService(ProductoRepository repositorio,
            ProveedorProductoRepository asociaciones,
            MovimientosCompraService movimientosCompra,
            MovimientosVentaService movimientosVenta,
            ContextoActual contexto) {
        this.repositorio = repositorio;
        this.asociaciones = asociaciones;
        this.movimientosCompra = movimientosCompra;
        this.movimientosVenta = movimientosVenta;
        this.contexto = contexto;
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest datos) {
        Producto producto = new Producto();
        producto.setIdEmpresa(contexto.getIdEmpresa());
        copiarDatos(producto, datos);
        return ProductoResponse.desde(repositorio.save(producto));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return aRespuestas(repositorio.findByIdEmpresa(contexto.getIdEmpresa()));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long idProducto) {
        return ProductoResponse.desde(buscarProducto(idProducto));
    }

    @Transactional
    public ProductoResponse actualizar(Long idProducto, ProductoRequest datos) {
        Producto producto = buscarProducto(idProducto);
        copiarDatos(producto, datos);
        return ProductoResponse.desde(repositorio.save(producto));
    }

    // RN-12: no se elimina un producto con movimientos. RN-14: las asociaciones
    // guardan solo el id del producto (sin clave foránea), por eso se borran aquí
    // para no dejarlas apuntando a un producto que ya no existe.
    @Transactional
    public void eliminar(Long idProducto) {
        Producto producto = buscarProducto(idProducto);
        validarSinOrdenesDeCompra(idProducto, producto);
        validarSinFacturas(idProducto, producto);
        asociaciones.deleteByIdEmpresaAndIdProducto(contexto.getIdEmpresa(), idProducto);
        repositorio.delete(producto);
    }

    // RN-04: stock bajo es stock < stockMinimo (la consulta del repositorio usa "<"
    // estricto).
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarStockBajo() {
        return aRespuestas(repositorio.buscarConStockBajo(contexto.getIdEmpresa()));
    }

    @Transactional
    public void incrementarStock(Long idProducto, int cantidad) {
        validarCantidad(cantidad);
        Producto producto = buscarProducto(idProducto);
        producto.setStock(producto.getStock() + cantidad);
        repositorio.save(producto);
    }

    // RN-01: este es el único lugar donde se valida que haya stock suficiente.
    @Transactional
    public void decrementarStock(Long idProducto, int cantidad) {
        validarCantidad(cantidad);
        Producto producto = buscarProducto(idProducto);
        validarStockSuficiente(producto, cantidad);
        producto.setStock(producto.getStock() - cantidad);
        repositorio.save(producto);
    }

    // RN-05: filtrar por empresa en la consulta evita revelar si el producto existe
    // en otra.
    private Producto buscarProducto(Long idProducto) {
        return repositorio.findByIdAndIdEmpresa(idProducto, contexto.getIdEmpresa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado: " + idProducto));
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
    }

    private void validarStockSuficiente(Producto producto, int cantidad) {
        if (producto.getStock() < cantidad) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para '" + producto.getNombre() + "': disponible "
                            + producto.getStock() + ", solicitado " + cantidad);
        }
    }

    // RN-12 (órdenes de compra).
    private void validarSinOrdenesDeCompra(Long idProducto, Producto producto) {
        if (movimientosCompra.productoTieneOrdenes(idProducto)) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el producto '" + producto.getNombre()
                            + "' porque tiene órdenes de compra");
        }
    }

    // RN-12 (facturas).
    private void validarSinFacturas(Long idProducto, Producto producto) {
        if (movimientosVenta.productoTieneFacturas(idProducto)) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el producto '" + producto.getNombre()
                            + "' porque tiene facturas");
        }
    }

    private void copiarDatos(Producto producto, ProductoRequest datos) {
        producto.setNombre(datos.nombre());
        producto.setPrecioVenta(datos.precioVenta());
        producto.setStock(datos.stock());
        producto.setStockMinimo(datos.stockMinimo());
    }

    private List<ProductoResponse> aRespuestas(List<Producto> productos) {
        return productos.stream().map(ProductoResponse::desde).toList();
    }
}