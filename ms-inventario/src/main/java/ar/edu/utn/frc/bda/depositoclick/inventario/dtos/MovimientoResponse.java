package ar.edu.utn.frc.bda.depositoclick.inventario.dtos;

import ar.edu.utn.frc.bda.depositoclick.inventario.models.MotivoMovimiento;
import ar.edu.utn.frc.bda.depositoclick.inventario.models.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoResponse {
    private Long id;
    private TipoMovimiento tipo;
    private MotivoMovimiento motivo;
    private Long depositoId;
    private String depositoCodigo;
    private Long articuloId;
    private String sku;
    private String lote;
    private LocalDate fechaVencimiento;
    private BigDecimal cantidad;
    private BigDecimal saldoLote;
    private LocalDateTime fecha;
    private String usuario;
    private String referencia;
    private String observacion;
}
