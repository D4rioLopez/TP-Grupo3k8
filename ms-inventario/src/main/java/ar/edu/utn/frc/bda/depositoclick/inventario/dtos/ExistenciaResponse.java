package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistenciaResponse {
    private Long depositoId;
    private String depositoCodigo;
    private Long articuloId;
    private String sku;
    private String lote;
    private LocalDate fechaVencimiento;
    private BigDecimal cantidad;
    private boolean vencida;
    private Long diasParaVencer;
}
