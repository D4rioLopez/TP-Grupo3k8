package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Categoria de articulos dentro de un rubro. Es la pieza clave del "polimorfismo" del sistema:
 * - define QUE atributos tienen sus articulos (talle y color para remeras, droga y receta para medicamentos)
 * - define COMO se controla su stock (si se maneja por lote y/o por fecha de vencimiento).
 *
 * Usamos @Getter/@Setter y no @Data porque tiene una relacion bidireccional con sus atributos
 * (el equals/hashCode de @Data entraria en un bucle infinito).
 */
@Entity
@Table(name = "categorias", uniqueConstraints = @UniqueConstraint(columnNames = {"rubro_id", "nombre"}))
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rubro_id")
    private Rubro rubro;

    // el stock de sus articulos se separa por lote (trazabilidad)
    @Column(nullable = false)
    private boolean controlaLote;

    // el stock de sus articulos tiene fecha de vencimiento (se despacha FEFO)
    @Column(nullable = false)
    private boolean controlaVencimiento;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AtributoCategoria> atributos = new ArrayList<>();

    public void agregarAtributo(AtributoCategoria atributo) {
        atributo.setCategoria(this);
        atributos.add(atributo);
    }
}
