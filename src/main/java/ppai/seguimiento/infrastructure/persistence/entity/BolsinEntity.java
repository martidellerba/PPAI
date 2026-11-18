package ppai.seguimiento.infrastructure.persistence.entity;

import ppai.seguimiento.domain.model.EstadoBolsin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "bolsin")
public class BolsinEntity {

    @Id
    @Column(name = "numero_bolsin")
    private Integer numeroBolsin;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_precinto", nullable = false)
    private String numeroPrecinto;

    @Column(nullable = false)
    private Double peso;

    @Column(name = "codigo_comision_origen", nullable = false)
    private String codigoComisionMedicaOrigen;

    @Column(name = "nombre_comision_origen", nullable = false)
    private String nombreComisionMedicaOrigen;

    @Column(name = "codigo_comision_destino", nullable = false)
    private String codigoComisionMedicaDestino;

    @Column(name = "nombre_comision_destino", nullable = false)
    private String nombreComisionMedicaDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoBolsin estado;

    protected BolsinEntity() {
    }

    public BolsinEntity(
            Integer numeroBolsin,
            LocalDate fecha,
            String numeroPrecinto,
            Double peso,
            String codigoComisionMedicaOrigen,
            String nombreComisionMedicaOrigen,
            String codigoComisionMedicaDestino,
            String nombreComisionMedicaDestino,
            EstadoBolsin estado
    ) {
        this.numeroBolsin = numeroBolsin;
        this.fecha = fecha;
        this.numeroPrecinto = numeroPrecinto;
        this.peso = peso;
        this.codigoComisionMedicaOrigen = codigoComisionMedicaOrigen;
        this.nombreComisionMedicaOrigen = nombreComisionMedicaOrigen;
        this.codigoComisionMedicaDestino = codigoComisionMedicaDestino;
        this.nombreComisionMedicaDestino = nombreComisionMedicaDestino;
        this.estado = estado;
    }

    public Integer getNumeroBolsin() {
        return numeroBolsin;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getNumeroPrecinto() {
        return numeroPrecinto;
    }

    public Double getPeso() {
        return peso;
    }

    public String getCodigoComisionMedicaOrigen() {
        return codigoComisionMedicaOrigen;
    }

    public String getNombreComisionMedicaOrigen() {
        return nombreComisionMedicaOrigen;
    }

    public String getCodigoComisionMedicaDestino() {
        return codigoComisionMedicaDestino;
    }

    public String getNombreComisionMedicaDestino() {
        return nombreComisionMedicaDestino;
    }

    public EstadoBolsin getEstado() {
        return estado;
    }
}
