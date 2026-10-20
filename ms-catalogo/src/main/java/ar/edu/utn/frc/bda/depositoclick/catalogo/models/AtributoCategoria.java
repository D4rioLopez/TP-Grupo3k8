package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.util.Arrays;
import java.util.List;

/** Definicion de un atributo dinamico que deben (o pueden) tener los articulos de una categoria. */
@Entity
@Table(name = "atributos_categoria", uniqueConstraints = @UniqueConstraint(columnNames = {"categoria_id", "nombre"}))
@Getter
@Setter
@NoArgsConstructor
public class AtributoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtributo tipo;

    @Column(nullable = false)
    private boolean obligatorio;

    // solo para tipo OPCION, separadas por coma. Ej: "S,M,L,XL"
    @Column(length = 500)
    private String opciones;

    public AtributoCategoria(String nombre, TipoAtributo tipo, boolean obligatorio, String opciones) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.obligatorio = obligatorio;
        this.opciones = opciones;
    }

    public List<String> getListaOpciones() {
        if (opciones == null || opciones.isBlank()) {
            return List.of();
        }
        return Arrays.stream(opciones.split(",")).map(String::trim).toList();
    }
}
