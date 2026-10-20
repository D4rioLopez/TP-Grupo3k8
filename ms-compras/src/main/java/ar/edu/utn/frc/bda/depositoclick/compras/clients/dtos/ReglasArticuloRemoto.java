package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Respuesta de ms-catalogo (/articulos/{id}/reglas-stock). Clase propia de Compras, no compartida. */
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
