package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Un elemento de la respuesta de https://api.argentinadatos.com/v1/feriados/{anio}
 * Ej: {"fecha":"2026-03-23","tipo":"puente","nombre":"Puente turistico no laborable"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeriadoArgentinaDatos {
    private LocalDate fecha;
    private String tipo;
    private String nombre;
}
