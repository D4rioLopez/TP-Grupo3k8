package ar.edu.utn.frc.bda.depositoclick.compras.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Que articulos vende cada proveedor y a que precio. Es informacion de COMPRAS (no del catalogo):
 * el catalogo no sabe nada de proveedores ni de precios de costo.
 */
@Entity
@Table(name = "articulos_proveedor", uniqueConstraints = @UniqueConstraint(columnNames = {"proveedor_id", "articulo_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ArticuloProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    // copias del catalogo, solo para mostrar
    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombreArticulo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    // proveedor elegido por defecto para reponer este articulo (uno solo por articulo)
    @Column(nullable = false)
    private boolean preferido;
}
