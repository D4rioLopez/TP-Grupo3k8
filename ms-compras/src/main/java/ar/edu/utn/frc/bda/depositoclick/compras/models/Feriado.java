package ar.edu.utn.frc.bda.depositoclick.compras.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Feriado obtenido de una API publica y guardado localmente (cache persistente). */
@Entity
@Table(name = "feriados", indexes = @Index(name = "idx_feriado_pais_anio", columnList = "pais, anio"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2)
    private String pais;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 120)
    private String nombre;
}
