package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Resultado de una operacion de stock (puede generar varios movimientos, uno por lote). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionResponse {
    private String referencia;
    private String tipoOperacion;
    private LocalDateTime fecha;
    private String usuario;

    @Schema(description = "true si la referencia ya habia sido procesada: no se volvio a aplicar el stock")
    private boolean repetida;

    // advertencias que no impiden la operacion (ej: se valido con la copia local del catalogo)
    private List<String> advertencias;

    private List<MovimientoResponse> movimientos;
}
