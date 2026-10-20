package ar.edu.utn.frc.bda.depositoclick.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato unico de error para TODOS los microservicios.
 * Que todos respondan igual le simplifica la vida al cliente (y a los otros servicios
 * que nos consumen, que pueden leer el "mensaje" y propagarlo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private String path;
    // errores por campo (solo para validaciones de entrada)
    private Map<String, String> detalles;
}
