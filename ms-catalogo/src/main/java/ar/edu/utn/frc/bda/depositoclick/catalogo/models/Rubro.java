package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rubro del negocio: Alimentos, Indumentaria, Ferreteria, Farmacia... */
@Entity
@Table(name = "rubros")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
