package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class OrdenRequest {

    @NotNull(message = "El proveedor es obligatorio")
    @Schema(example = "1")
    private Long proveedorId;

    @NotNull(message = "El deposito de destino es obligatorio")
    @Schema(example = "1")
    private Long depositoDestinoId;

    @FutureOrPresent(message = "La fecha de entrega solicitada no puede ser pasada")
    @Schema(description = "Opcional. Al emitir se valida que sea un dia habil y que el proveedor llegue a cumplirla")
    private LocalDate fechaEntregaSolicitada;

    @Size(max = 255)
    private String observacion;

    @NotEmpty(message = "La orden debe tener al menos un item")
    @Size(max = 100, message = "Como maximo 100 items por orden")
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
        @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 3 decimales")
        @Schema(example = "48")
        private BigDecimal cantidad;

        @Positive(message = "El precio debe ser mayor a cero")
        @Digits(integer = 12, fraction = 2, message = "El precio admite hasta 2 decimales")
        @Schema(description = "Opcional: si no se informa se usa el precio pactado con el proveedor")
        private BigDecimal precioUnitario;
    }
}
