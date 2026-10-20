package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.TipoAtributo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtributoDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,39}$",
            message = "El nombre del atributo debe empezar con una letra y solo tener letras, numeros o _ (max 40)")
    @Schema(example = "talle")
    private String nombre;

    @NotNull(message = "El tipo del atributo es obligatorio")
    @Schema(example = "OPCION")
    private TipoAtributo tipo;

    private boolean obligatorio;

    // solo para tipo OPCION
    @Schema(example = "[\"S\", \"M\", \"L\", \"XL\"]")
    private List<String> opciones;
}
