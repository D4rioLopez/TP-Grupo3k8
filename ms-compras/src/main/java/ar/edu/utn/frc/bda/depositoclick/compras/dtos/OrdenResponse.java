package ar.edu.utn.frc.bda.depositoclick.compras.dtos;

import ar.edu.utn.frc.bda.depositoclick.compras.models.EstadoOrden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenResponse {
    private Long id;
    private EstadoOrden estado;
    private Long proveedorId;
    private String proveedorRazonSocial;
    private int leadTimeDiasHabiles;
    private Long depositoDestinoId;
    private String depositoCodigo;
    private String paisDeposito;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEmision;
    private LocalDate fechaEntregaSolicitada;
    private LocalDate fechaEntregaEstimada;
    private String fuenteCalendario;
    private Boolean calendarioConfiable;
    private List<String> feriadosConsiderados;
    private List<String> advertencias;
    private LocalDateTime fechaCancelacion;
    private String motivoCancelacion;
    private String observacion;
    private String creadoPor;
    private BigDecimal total;
    private List<Detalle> detalles;
    private List<RecepcionInfo> recepciones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detalle {
        private Long articuloId;
        private String sku;
        private String nombreArticulo;
        private String unidadMedida;
        private BigDecimal cantidadPedida;
        private BigDecimal cantidadRecibida;
        private BigDecimal pendiente;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecepcionInfo {
        private int numero;
        private LocalDateTime fecha;
        private String usuario;
        private String referenciaInventario;
        private String observacion;
        private List<ItemRecibido> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRecibido {
        private Long articuloId;
        private BigDecimal cantidad;
        private String lote;
        private LocalDate fechaVencimiento;
    }
}
