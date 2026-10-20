package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que efectivamente llego del proveedor (puede ser menos de lo pedido: recepcion parcial). */
@Data
@NoArgsConstructor
public class RecepcionRequest {

    @Size(max = 255)
    @Schema(example = "Remito 0001-00004567")
    private String observacion;

    @NotEmpty(message = "Debe informar al menos un item recibido")
    @Valid
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull(message = "El articulo es obligatorio")
        @Schema(example = "1")
        private Long articuloId;

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 11, fraction = 3)
        @Schema(example = "24")
        private BigDecimal cantidad;

        @Pattern(regexp = "^[A-Za-z0-9._/-]{1,40}$", message = "El lote solo admite letras, numeros y . _ / - (max 40)")
        @Schema(example = "L-2611-A", description = "Obligatorio si el articulo controla lote")
        private String lote;

        @Schema(example = "2026-12-20", description = "Obligatoria si el articulo controla vencimiento")
        private LocalDate fechaVencimiento;
    }
}
