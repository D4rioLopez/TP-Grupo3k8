package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Resultado de sumar N dias habiles a una fecha en un pais. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoEntregaResponse {
    private String pais;
    private LocalDate desde;
    private int diasHabiles;
    private LocalDate fechaEntrega;
    // dias de calendario que hay que esperar (lo que importa para el stock)
    private long diasCorridos;
    // feriados que cayeron en el medio y se saltearon (ej: "2026-11-23 Dia de la Soberania Nacional")
    private List<String> feriadosSalteados;
    private String fuente;
    private boolean confiable;
    private List<String> advertencias;
}
