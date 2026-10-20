package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeriadosResponse {
    private String pais;
    private int anio;
    private String fuente;
    private boolean confiable;
    private String advertencia;
    private List<Item> feriados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private LocalDate fecha;
        private String nombre;
    }
}
