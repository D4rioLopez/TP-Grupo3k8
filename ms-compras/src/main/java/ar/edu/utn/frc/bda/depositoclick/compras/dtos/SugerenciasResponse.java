package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resultado del calculo de reposicion de un deposito. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SugerenciasResponse {
    private Long depositoId;
    private String depositoCodigo;
    private String pais;
    private LocalDate fechaCalculo;
    private int diasCobertura;
    private int diasHistoriaConsumo;
    private boolean calendarioConfiable;
    private List<String> advertencias;
    private List<Sugerencia> sugerencias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sugerencia {
        private Long articuloId;
        private String sku;
        private String nombre;
        private String unidadMedida;
        private BigDecimal stockDisponible;
        // lo que ya esta pedido en ordenes emitidas que todavia no llego
        private BigDecimal pendienteEnOrdenes;
        private BigDecimal consumoDiarioPromedio;
        private BigDecimal stockMinimo;
        private Long proveedorId;
        private String proveedorRazonSocial;
        private Integer leadTimeDiasHabiles;
        private Long diasCorridosHastaEntrega;
        private LocalDate fechaEstimadaEntrega;
        private BigDecimal puntoDePedido;
        private BigDecimal cantidadSugerida;
        private BigDecimal precioUnitario;
        private String explicacion;
    }
}
