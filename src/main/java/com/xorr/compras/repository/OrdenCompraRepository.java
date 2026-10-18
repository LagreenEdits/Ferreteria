package com.xorr.compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xorr.compras.entity.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<OrdenCompra> findByIdEmpresa(Long idEmpresa);

    // RN-12: el producto vive en el detalle, no en la orden.
    @Query("select case when count(d) > 0 then true else false end "
            + "from OrdenCompra o join o.detalles d "
            + "where o.idEmpresa = :idEmpresa and d.idProducto = :idProducto")
    boolean existeConProducto(@Param("idEmpresa") Long idEmpresa, @Param("idProducto") Long idProducto);

    // RN-12
    boolean existsByIdEmpresaAndIdProveedor(Long idEmpresa, Long idProveedor);
}