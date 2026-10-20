package ar.edu.utn.frc.bda.depositoclick.catalogo.models;

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

/** Valor concreto de un atributo dinamico para un articulo (ej: talle = "M"). */
@Entity
@Table(name = "valores_atributo")
@Getter
@Setter
@NoArgsConstructor
public class ValorAtributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "articulo_id")
    private Articulo articulo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "atributo_id")
    private AtributoCategoria atributo;

    // se guarda como texto ya validado segun el tipo del atributo
    @Column(nullable = false, length = 255)
    private String valor;

    public ValorAtributo(AtributoCategoria atributo, String valor) {
        this.atributo = atributo;
        this.valor = valor;
    }
}
