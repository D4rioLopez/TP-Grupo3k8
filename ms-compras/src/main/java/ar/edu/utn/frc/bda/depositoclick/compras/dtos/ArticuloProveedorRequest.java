package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ArticuloProveedorRequest {

    @NotNull(message = "El articulo es obligatorio")
    @Schema(example = "2")
    private Long articuloId;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    @Digits(integer = 12, fraction = 2, message = "El precio admite hasta 2 decimales")
    @Schema(example = "1350.00")
    private BigDecimal precioUnitario;

    @Schema(description = "Si es el proveedor que se usa por defecto para reponer este articulo")
    private boolean preferido;
}
