package ppai.seguimiento.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comision_medica")
public class ComisionMedicaEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "email_gerente")
    private String emailGerente;

    protected ComisionMedicaEntity() {
    }

    public ComisionMedicaEntity(String codigo, String nombre, String emailGerente) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.emailGerente = emailGerente;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getEmailGerente() { return emailGerente; }
}
