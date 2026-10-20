package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Contrato que el catalogo le ofrece a los otros microservicios (Inventario y Compras):
 * solo lo necesario para decidir COMO se mueve el stock de un articulo.
 * La unidad viaja como String (y no como el enum) para que los consumidores no dependan
 * de nuestras clases internas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglasStockResponse {
    private Long articuloId;
    private String sku;
    private String nombre;
    private String unidadMedida;
    private boolean permiteDecimales;
    private boolean controlaLote;
    private boolean controlaVencimiento;
    private BigDecimal stockMinimo;
    private boolean activo;
}
