package com.xorr.inventario.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.OperacionInvalidaException;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProveedorProductoRequest;
import com.xorr.inventario.dto.ProveedorProductoResponse;
import com.xorr.inventario.entity.ProveedorProducto;
import com.xorr.inventario.repository.ProveedorProductoRepository;

@Service
public class ProveedorProductoService {

    private static final String MENSAJE_DUPLICADA = "Ya existe esa asociación";

    private final ProveedorProductoRepository repositorio;
    private final ContextoActual contexto;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;

    public ProveedorProductoService(ProveedorProductoRepository repositorio,
            ContextoActual contexto,
            ProveedorService proveedorService,
            ProductoService productoService) {
        this.repositorio = repositorio;
        this.contexto = contexto;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
    }

    @Transactional
    public ProveedorProductoResponse crear(ProveedorProductoRequest datos) {
        validarReferencias(datos);
        validarSinDuplicado(datos);
        ProveedorProducto asociacion = new ProveedorProducto();
        asociacion.setIdEmpresa(contexto.getIdEmpresa());
        copiarDatos(asociacion, datos);
        return ProveedorProductoResponse.desde(repositorio.save(asociacion));
    }

    @Transactional(readOnly = true)
    public List<ProveedorProductoResponse> listar() {
        return repositorio.findByIdEmpresa(contexto.getIdEmpresa()).stream()
                .map(ProveedorProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorProductoResponse obtener(Long idAsociacion) {
        return ProveedorProductoResponse.desde(buscarAsociacion(idAsociacion));
    }

    // RN-09: solo se compran productos asociados al proveedor, al precio de esa
    // asociación.
    @Transactional(readOnly = true)
    public BigDecimal obtenerPrecioCompra(Long idProveedor, Long idProducto) {
        return repositorio.findByIdEmpresaAndIdProveedorAndIdProducto(
                contexto.getIdEmpresa(), idProveedor, idProducto)
                .map(ProveedorProducto::getPrecioCompra)
                .orElseThrow(() -> new OperacionInvalidaException(
                        "El producto " + idProducto + " no está asociado al proveedor " + idProveedor));
    }

    @Transactional
    public ProveedorProductoResponse actualizar(Long idAsociacion, ProveedorProductoRequest datos) {
        ProveedorProducto asociacion = buscarAsociacion(idAsociacion);
        validarReferencias(datos);
        validarSinDuplicadoAlActualizar(datos, idAsociacion);
        copiarDatos(asociacion, datos);
        return ProveedorProductoResponse.desde(repositorio.save(asociacion));
    }

    @Transactional
    public void eliminar(Long idAsociacion) {
        repositorio.delete(buscarAsociacion(idAsociacion));
    }

    // RN-05: los servicios lanzan "no encontrado" si el proveedor o el producto son
    // de otra empresa.
    private void validarReferencias(ProveedorProductoRequest datos) {
        proveedorService.obtener(datos.idProveedor());
        productoService.obtener(datos.idProducto());
    }

    private void validarSinDuplicado(ProveedorProductoRequest datos) {
        boolean existe = repositorio.existsByIdEmpresaAndIdProveedorAndIdProducto(
                contexto.getIdEmpresa(), datos.idProveedor(), datos.idProducto());
        rechazarSiExiste(existe);
    }

    // Al actualizar se excluye la propia fila: cambiar solo el precio no es un
    // duplicado.
    private void validarSinDuplicadoAlActualizar(ProveedorProductoRequest datos, Long idAsociacion) {
        boolean existe = repositorio.existsByIdEmpresaAndIdProveedorAndIdProductoAndIdNot(
                contexto.getIdEmpresa(), datos.idProveedor(), datos.idProducto(), idAsociacion);
        rechazarSiExiste(existe);
    }

    private void rechazarSiExiste(boolean existe) {
        if (existe) {
            throw new OperacionInvalidaException(MENSAJE_DUPLICADA);
        }
    }

    private ProveedorProducto buscarAsociacion(Long idAsociacion) {
        return repositorio.findByIdAndIdEmpresa(idAsociacion, contexto.getIdEmpresa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Asociación proveedor-producto no encontrada: " + idAsociacion));
    }

    private void copiarDatos(ProveedorProducto asociacion, ProveedorProductoRequest datos) {
        asociacion.setIdProveedor(datos.idProveedor());
        asociacion.setIdProducto(datos.idProducto());
        asociacion.setPrecioCompra(datos.precioCompra());
    }
}