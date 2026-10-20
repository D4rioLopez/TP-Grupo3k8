package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.MotivoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class IngresoRequest {

    @NotNull(message = "El deposito es obligatorio")
    @Schema(example = "1")
    private Long depositoId;

    @NotNull(message = "El motivo es obligatorio")
    @Schema(example = "STOCK_INICIAL", description = "COMPRA, DEVOLUCION_CLIENTE o STOCK_INICIAL")
    private MotivoMovimiento motivo;

    @Pattern(regexp = "^[A-Za-z0-9._/-]{3,60}$", message = "La referencia solo admite letras, numeros y . _ / - (3 a 60)")
    @Schema(example = "REMITO-0001-00012345",
            description = "Identificador unico de la operacion. Si se repite, no se vuelve a aplicar (idempotencia)")
    private String referencia;

    @Size(max = 255, message = "La observacion no puede superar los 255 caracteres")
    private String observacion;

    @NotEmpty(message = "Debe informar al menos un item")
    @Size(max = 200, message = "Como maximo 200 items por operacion")
    @Valid
    private List<ItemIngresoDTO> items;
}
