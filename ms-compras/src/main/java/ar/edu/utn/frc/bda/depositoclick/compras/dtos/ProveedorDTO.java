package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ProveedorDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El CUIT es obligatorio")
    @Pattern(regexp = "^\\d{2}-?\\d{8}-?\\d$", message = "El CUIT debe tener 11 digitos (con o sin guiones)")
    @Schema(example = "30-71555444-1")
    private String cuit;

    @NotBlank(message = "La razon social es obligatoria")
    @Size(max = 120, message = "La razon social no puede superar los 120 caracteres")
    @Schema(example = "Lacteos del Sur SRL")
    private String razonSocial;

    @Email(message = "El email no es valido")
    @Size(max = 120)
    @Schema(example = "ventas@lacteosdelsur.com.ar")
    private String email;

    @Size(max = 30)
    private String telefono;

    @Min(value = 0, message = "El plazo de entrega no puede ser negativo")
    @Max(value = 120, message = "El plazo de entrega no puede superar los 120 dias habiles")
    @Schema(example = "3", description = "Dias habiles que tarda en entregar desde la emision de la orden")
    private int leadTimeDiasHabiles;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private boolean activo;
}
