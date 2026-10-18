package com.xorr.ventas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xorr.ventas.entity.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByIdAndIdEmpresa(Long id, Long idEmpresa);

    List<Factura> findByIdEmpresa(Long idEmpresa);

    // RN-12: el producto vive en el detalle, no en la factura.
    @Query("select case when count(d) > 0 then true else false end "
            + "from Factura f join f.detalles d "
            + "where f.idEmpresa = :idEmpresa and d.idProducto = :idProducto")
    boolean existeConProducto(@Param("idEmpresa") Long idEmpresa, @Param("idProducto") Long idProducto);
}