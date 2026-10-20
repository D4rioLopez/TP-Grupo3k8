package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoriaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    @Schema(example = "Remeras")
    private String nombre;

    @NotNull(message = "El rubro es obligatorio")
    @Schema(example = "2")
    private Long rubroId;

    @Schema(description = "Si el stock de sus articulos se controla por lote")
    private boolean controlaLote;

    @Schema(description = "Si el stock de sus articulos tiene vencimiento (se despacha FEFO)")
    private boolean controlaVencimiento;

    @Valid
    private List<AtributoDTO> atributos = new ArrayList<>();
}
