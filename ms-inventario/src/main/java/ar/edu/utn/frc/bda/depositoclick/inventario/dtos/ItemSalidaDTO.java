package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Item de un egreso o transferencia. Si no se indica lote, el sistema elige por FEFO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalidaDTO {

    @NotNull(message = "El articulo es obligatorio")
    @Schema(example = "1")
    private Long articuloId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
    @Schema(example = "3")
    private BigDecimal cantidad;

    @Pattern(regexp = "^[A-Za-z0-9._/-]{1,40}$", message = "El lote solo admite letras, numeros y . _ / - (max 40)")
    @Schema(description = "Opcional: si no se informa se despacha primero lo que vence antes (FEFO)")
    private String lote;
}
