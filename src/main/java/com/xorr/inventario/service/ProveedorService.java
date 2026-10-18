package com.xorr.inventario.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xorr.common.ContextoActual;
import com.xorr.common.RecursoNoEncontradoException;
import com.xorr.inventario.dto.ProveedorRequest;
import com.xorr.inventario.dto.ProveedorResponse;
import com.xorr.inventario.entity.Proveedor;
import com.xorr.inventario.repository.ProveedorProductoRepository;
import com.xorr.inventario.repository.ProveedorRepository;

@Service
public class ProveedorService {

    private final ProveedorRepository repositorio;
    private final ProveedorProductoRepository asociaciones;
    private final ContextoActual contexto;

    public ProveedorService(ProveedorRepository repositorio,
            ProveedorProductoRepository asociaciones,
            ContextoActual contexto) {
        this.repositorio = repositorio;
        this.asociaciones = asociaciones;
        this.contexto = contexto;
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest datos) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdEmpresa(contexto.getIdEmpresa());
        copiarDatos(proveedor, datos);
        return ProveedorResponse.desde(repositorio.save(proveedor));
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return repositorio.findByIdEmpresa(contexto.getIdEmpresa()).stream()
                .map(ProveedorResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long idProveedor) {
        return ProveedorResponse.desde(buscarProveedor(idProveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Long idProveedor, ProveedorRequest datos) {
        Proveedor proveedor = buscarProveedor(idProveedor);
        copiarDatos(proveedor, datos);
        return ProveedorResponse.desde(repositorio.save(proveedor));
    }

    // Las asociaciones guardan solo el id del proveedor (sin clave foránea), por
    // eso se
    // borran aquí para no dejarlas apuntando a un proveedor que ya no existe.
    @Transactional
    public void eliminar(Long idProveedor) {
        Proveedor proveedor = buscarProveedor(idProveedor);
        asociaciones.deleteByIdEmpresaAndIdProveedor(contexto.getIdEmpresa(), idProveedor);
        repositorio.delete(proveedor);
    }

    // RN-05: un proveedor de otra empresa se trata como no encontrado.
    private Proveedor buscarProveedor(Long idProveedor) {
        return repositorio.findByIdAndIdEmpresa(idProveedor, contexto.getIdEmpresa())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado: " + idProveedor));
    }

    private void copiarDatos(Proveedor proveedor, ProveedorRequest datos) {
        proveedor.setNombre(datos.nombre());
        proveedor.setTelefono(datos.telefono());
    }
}