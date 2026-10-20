package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ArticuloRequest {

    @NotBlank(message = "El SKU es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$", message = "El SKU debe tener entre 3 y 30 caracteres: letras, numeros o guiones")
    @Schema(example = "IND-003")
    private String sku;

    @Pattern(regexp = "^(\\d{8}|\\d{12,14})$", message = "El codigo de barras debe ser un GTIN de 8, 12, 13 o 14 digitos")
    @Schema(example = "7790001000019")
    private String codigoBarras;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Schema(example = "Remera algodon roja talle S")
    private String nombre;

    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String descripcion;

    @NotNull(message = "La categoria es obligatoria")
    @Schema(example = "4")
    private Long categoriaId;

    @NotNull(message = "La unidad de medida es obligatoria")
    @Schema(example = "UNIDAD")
    private UnidadMedida unidadMedida;

    @NotNull(message = "El stock minimo es obligatorio")
    @PositiveOrZero(message = "El stock minimo no puede ser negativo")
    @Digits(integer = 11, fraction = 3, message = "El stock minimo admite hasta 3 decimales")
    @Schema(example = "5")
    private BigDecimal stockMinimo;

    @Schema(description = "Valores de los atributos dinamicos definidos por la categoria",
            example = "{\"talle\": \"S\", \"color\": \"Rojo\"}")
    private Map<String, String> atributos = new LinkedHashMap<>();
}
