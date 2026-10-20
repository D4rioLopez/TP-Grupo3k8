package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que le mandamos a ms-inventario (POST /movimientos/ingresos) al recibir mercaderia. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoRemotoRequest {
    private Long depositoId;
    private String motivo;
    private String referencia;
    private String observacion;
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long articuloId;
        private BigDecimal cantidad;
        private String lote;
        private LocalDate fechaVencimiento;
    }
}
