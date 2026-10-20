package ar.edu.utn.frc.bda.depositoclick.inventario.models;

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

/**
 * Deposito fisico donde se guarda la mercaderia. Un mismo negocio puede tener varios
 * (incluso en distintos paises: el pais define el calendario de feriados que usa Compras
 * para estimar cuando llega la mercaderia).
 */
@Entity
@Table(name = "depositos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 150)
    private String direccion;

    // codigo ISO 3166-1 alfa-2 (AR, UY, CL...)
    @Column(nullable = false, length = 2)
    private String pais;

    @Column(nullable = false)
    private boolean activo;
}
