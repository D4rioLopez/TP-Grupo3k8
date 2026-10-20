package ar.edu.utn.frc.bda.depositoclick.inventario.clients;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lo que nos responde ms-catalogo en /api/v1/articulos/{id}/reglas-stock.
 * Es una clase PROPIA de inventario (no compartimos los DTOs del catalogo): asi, si el catalogo
 * agrega campos nuevos, no nos rompe. La unidad viaja como String, no como el enum del otro servicio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglasArticuloRemoto {
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
