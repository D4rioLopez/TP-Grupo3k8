package ar.edu.utn.frc.bda.depositoclick.inventario.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cantidad de un articulo, en un deposito, para un lote. Es el "saldo" del stock.
 * Nunca se modifica a mano: solo cambia a traves de un Movimiento (que queda como historia).
 *
 * @Version: bloqueo optimista. Si dos cajas venden el ultimo yogur al mismo tiempo, una de las
 * dos transacciones falla (409) en lugar de dejar el stock en -1.
 */
@Entity
@Table(name = "existencias", uniqueConstraints = @UniqueConstraint(columnNames = {"deposito_id", "articulo_id", "lote"}))
@Getter
@Setter
@NoArgsConstructor
public class Existencia {

    // valor de "lote" para articulos que no se controlan por lote
    public static final String SIN_LOTE = "SIN-LOTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    // referencia al articulo del catalogo (otro microservicio): solo guardamos el id
    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    // copia del SKU solo para mostrar
    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 40)
    private String lote;

    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad = BigDecimal.ZERO;

    private LocalDateTime fechaUltimoMovimiento;

    @Version
    private Long version;

    public boolean estaVencida(LocalDate hoy) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(hoy);
    }
}
