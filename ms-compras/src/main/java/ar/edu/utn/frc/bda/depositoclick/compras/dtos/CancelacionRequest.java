package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionRequest {

    @NotBlank(message = "Debe indicar el motivo de la cancelacion")
    @Size(max = 255)
    @Schema(example = "El proveedor no tiene stock")
    private String motivo;
}
