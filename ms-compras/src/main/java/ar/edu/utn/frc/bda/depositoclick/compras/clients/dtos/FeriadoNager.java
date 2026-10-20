package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Un elemento de la respuesta de https://date.nager.at/api/v3/PublicHolidays/{anio}/{pais}
 * Ej: {"date":"2026-05-25","localName":"Dia de la Revolucion de Mayo","global":true,"types":["Public"]}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeriadoNager {
    private LocalDate date;
    private String localName;
    private String name;
    // true = feriado nacional; false = solo en algunas provincias/estados (no lo usamos)
    private boolean global;
    private List<String> types;
}
