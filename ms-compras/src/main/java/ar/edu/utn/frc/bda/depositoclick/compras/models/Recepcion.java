package ar.edu.utn.frc.bda.depositoclick.compras.models;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Una entrega (total o parcial) de mercaderia de una orden de compra. */
@Entity
@Table(name = "recepciones")
@Getter
@Setter
@NoArgsConstructor
public class Recepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_id")
    private OrdenCompra orden;

    // 1, 2, 3... dentro de la orden
    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 60)
    private String usuario;

    // referencia con la que se registro el ingreso en ms-inventario (OC-{id}-REC-{numero})
    @Column(nullable = false, unique = true, length = 60)
    private String referenciaInventario;

    @Column(length = 255)
    private String observacion;

    @OneToMany(mappedBy = "recepcion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecepcionItem> items = new ArrayList<>();

    public void agregarItem(RecepcionItem item) {
        item.setRecepcion(this);
        items.add(item);
    }
}
