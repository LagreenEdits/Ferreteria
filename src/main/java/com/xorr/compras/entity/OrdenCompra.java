package com.xorr.compras.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orden_compra")
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_empresa", nullable = false)
    private Long idEmpresa;

    @Column(name = "id_proveedor", nullable = false)
    private Long idProveedor;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrdenCompra estado;

    @Column(nullable = false)
    private BigDecimal total;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_orden", nullable = false)
    private List<DetalleOrdenCompra> detalles = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Long idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompra estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<DetalleOrdenCompra> getDetalles() {
        return detalles;
    }
}