package ar.edu.utn.frc.bda.depositoclick.compras.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

/** Feriados de un pais en un año, junto con de donde salieron. */
@Getter
@AllArgsConstructor
public class CalendarioAnual {

    public static final String FUENTE_NAGER = "NAGER_DATE";
    public static final String FUENTE_ARGENTINA_DATOS = "ARGENTINA_DATOS";
    public static final String FUENTE_SOLO_FINES_DE_SEMANA = "SOLO_FINES_DE_SEMANA";

    private final String pais;
    private final int anio;
    // fecha -> nombre del feriado
    private final Map<LocalDate, String> feriados;
    private final String fuente;
    // false = no se pudieron obtener feriados: solo se descuentan sabados y domingos
    private final boolean confiable;
    // aviso para mostrarle al usuario (null si todo salio bien)
    private final String advertencia;
}
