package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Articulo del catalogo. Es el "dato maestro": el catalogo es el unico dueño de esta informacion.
 * Inventario y Compras solo guardan su id (y alguna copia de lectura), nunca la modifican.
 */
@Entity
@Table(name = "articulos")
@Getter
@Setter
@NoArgsConstructor
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // codigo interno del negocio, unico e inmutable
    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    // GTIN / EAN (codigo de barras) opcional, unico si se informa
    @Column(unique = true, length = 14)
    private String codigoBarras;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadMedida unidadMedida;

    // stock de seguridad: por debajo de esto el articulo esta "en falta"
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private LocalDateTime fechaAlta;

    private LocalDateTime fechaModificacion;

    @OneToMany(mappedBy = "articulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorAtributo> valores = new ArrayList<>();

    public void reemplazarValores(List<ValorAtributo> nuevos) {
        valores.clear();
        for (ValorAtributo v : nuevos) {
            v.setArticulo(this);
            valores.add(v);
        }
    }

    public Map<String, String> getAtributosComoMapa() {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (ValorAtributo v : valores) {
            mapa.put(v.getAtributo().getNombre(), v.getValor());
        }
        return mapa;
    }
}
