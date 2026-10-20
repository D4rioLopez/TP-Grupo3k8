package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Stock consolidado de un articulo en todos los depositos (o en uno, si se filtra). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockArticuloResponse {
    private Long articuloId;
    private String sku;
    private String nombre;
    private String unidadMedida;
    private BigDecimal stockTotal;
    // lo que se puede despachar (excluye lo vencido)
    private BigDecimal stockDisponible;
    private BigDecimal stockVencido;
    private BigDecimal stockMinimo;
    private boolean bajoMinimo;
    private List<ExistenciaResponse> existencias;
}
