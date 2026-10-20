package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

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
public class TransferenciaRequest {

    @NotNull(message = "El deposito de origen es obligatorio")
    @Schema(example = "1")
    private Long depositoOrigenId;

    @NotNull(message = "El deposito de destino es obligatorio")
    @Schema(example = "2")
    private Long depositoDestinoId;

    @Pattern(regexp = "^[A-Za-z0-9._/-]{3,60}$", message = "La referencia solo admite letras, numeros y . _ / - (3 a 60)")
    private String referencia;

    @Size(max = 255, message = "La observacion no puede superar los 255 caracteres")
    private String observacion;

    @NotEmpty(message = "Debe informar al menos un item")
    @Size(max = 200, message = "Como maximo 200 items por operacion")
    @Valid
    private List<ItemSalidaDTO> items;
}
