package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Respuesta de ms-inventario (/stock/resumen-reposicion). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenReposicionRemoto {
    private Long articuloId;
    private String sku;
    private BigDecimal stockDisponible;
    private BigDecimal consumoDiarioPromedio;
}
