package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositoDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9-]{2,20}$", message = "El codigo debe tener entre 2 y 20 letras, numeros o guiones")
    @Schema(example = "DEP-ROS")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Schema(example = "Deposito Rosario")
    private String nombre;

    @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
    private String direccion;

    @NotBlank(message = "El pais es obligatorio")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "El pais debe ser un codigo ISO de 2 letras (AR, UY, CL...)")
    @Schema(example = "AR", description = "Define el calendario de feriados que se usa para estimar entregas")
    private String pais;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private boolean activo;
}
