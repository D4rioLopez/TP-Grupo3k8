package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Se usa tanto para crear/modificar (el id se ignora) como para responder. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RubroDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Schema(example = "Alimentos")
    private String nombre;

    @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
    @Schema(example = "Productos alimenticios envasados y frescos")
    private String descripcion;
}
