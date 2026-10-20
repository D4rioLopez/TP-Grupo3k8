package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.UnidadMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloResponse {
    private Long id;
    private String sku;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private String categoriaNombre;
    private String rubroNombre;
    private UnidadMedida unidadMedida;
    private BigDecimal stockMinimo;
    private boolean controlaLote;
    private boolean controlaVencimiento;
    private boolean activo;
    private Map<String, String> atributos;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaModificacion;
}
