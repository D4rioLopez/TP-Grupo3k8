package ar.edu.utn.frc.bda.depositoclick.compras.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Registro de cuando se descargaron los feriados de un pais/anio y de que fuente. */
@Entity
@Table(name = "calendarios_descargados", uniqueConstraints = @UniqueConstraint(columnNames = {"pais", "anio"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioDescargado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2)
    private String pais;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false, length = 40)
    private String fuente;

    @Column(nullable = false)
    private LocalDateTime fechaDescarga;
}
