package ar.edu.utn.frc.bda.depositoclick.compras.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalles_orden")
@Getter
@Setter
@NoArgsConstructor
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_id")
    private OrdenCompra orden;

    @Column(nullable = false)
    private Long articuloId;

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombreArticulo;

    @Column(nullable = false, length = 20)
    private String unidadMedida;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadPedida;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadRecibida = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    public BigDecimal getPendiente() {
        return cantidadPedida.subtract(cantidadRecibida);
    }

    public BigDecimal getSubtotal() {
        return cantidadPedida.multiply(precioUnitario);
    }
}
