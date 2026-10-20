package ar.edu.utn.frc.bda.depositoclick.inventario.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Copia LOCAL (de solo lectura) de las reglas de stock de un articulo del catalogo.
 * El dueño del dato sigue siendo ms-catalogo; esta copia se actualiza cada vez que lo consultamos
 * con exito y nos permite:
 *  - seguir operando un tiempo acotado si el catalogo no responde (modo degradado)
 *  - armar listados (stock bajo minimo, etc.) sin pedirle al catalogo articulo por articulo.
 */
@Entity
@Table(name = "articulos_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloSnapshot {

    @Id
    private Long articuloId;

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String unidadMedida;

    private boolean permiteDecimales;
    private boolean controlaLote;
    private boolean controlaVencimiento;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo;

    private boolean activo;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;
}
