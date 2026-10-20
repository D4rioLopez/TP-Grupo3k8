package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ajuste por inventario fisico: se informa lo que se CONTO y el sistema calcula la diferencia.
 */
@Data
@NoArgsConstructor
public class AjusteRequest {

    @NotNull(message = "El deposito es obligatorio")
    private Long depositoId;

    @NotNull(message = "El articulo es obligatorio")
    private Long articuloId;

    @Pattern(regexp = "^[A-Za-z0-9._/-]{1,40}$", message = "El lote solo admite letras, numeros y . _ / - (max 40)")
    private String lote;

    @Schema(description = "Solo si se ajusta un lote que todavia no existe en el deposito")
    private LocalDate fechaVencimiento;

    @NotNull(message = "La cantidad contada es obligatoria")
    @PositiveOrZero(message = "La cantidad contada no puede ser negativa")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    private BigDecimal cantidadContada;

    @NotBlank(message = "Debe explicar el motivo del ajuste")
    @Size(max = 255, message = "La observacion no puede superar los 255 caracteres")
    @Schema(example = "Conteo mensual de octubre: faltante por rotura no informada")
    private String observacion;
}
