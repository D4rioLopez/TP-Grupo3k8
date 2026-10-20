package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloProveedorResponse {
    private Long id;
    private Long proveedorId;
    private String proveedorRazonSocial;
    private Long articuloId;
    private String sku;
    private String nombreArticulo;
    private BigDecimal precioUnitario;
    private boolean preferido;
}
