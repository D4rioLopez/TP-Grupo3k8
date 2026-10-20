package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lo que Inventario le informa a Compras para calcular la reposicion de un deposito:
 * cuanto hay disponible y cuanto se consume por dia en promedio. El "cuanto pedir"
 * lo decide Compras (es responsabilidad suya, no del inventario).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenReposicionDTO {
    private Long articuloId;
    private String sku;
    private BigDecimal stockDisponible;
    private BigDecimal consumoPeriodo;
    private int diasPeriodo;
    private BigDecimal consumoDiarioPromedio;
}
