package ar.edu.utn.frc.bda.depositoclick.inventario.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Historia inmutable del stock: cada cambio de una Existencia genera un Movimiento.
 * La cantidad siempre es positiva; el "signo" lo da el tipo.
 */
@Entity
@Table(name = "movimientos", indexes = {
        @Index(name = "idx_mov_referencia", columnList = "referencia"),
        @Index(name = "idx_mov_articulo_fecha", columnList = "articulo_id, fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MotivoMovimiento motivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 40)
    private String lote;

    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    // cuanto quedo en la existencia (deposito + articulo + lote) despues del movimiento
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoLote;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // quien lo hizo (sale del JWT)
    @Column(nullable = false, length = 60)
    private String usuario;

    // agrupa los movimientos de una misma operacion y la hace idempotente
    @Column(nullable = false, length = 60)
    private String referencia;

    @Column(length = 255)
    private String observacion;
}
