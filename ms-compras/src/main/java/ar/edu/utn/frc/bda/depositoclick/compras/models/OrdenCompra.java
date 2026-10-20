package ar.edu.utn.frc.bda.depositoclick.compras.models;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    // el deposito vive en ms-inventario: guardamos el id y una copia de lo que necesitamos
    @Column(nullable = false)
    private Long depositoDestinoId;

    @Column(nullable = false, length = 20)
    private String depositoCodigo;

    // pais del deposito destino: define el calendario de feriados para estimar la entrega
    @Column(nullable = false, length = 2)
    private String paisDeposito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaEmision;

    // fecha en que el cliente (nosotros) pide que llegue; opcional
    private LocalDate fechaEntregaSolicitada;

    // calculada al emitir: fecha de emision + lead time del proveedor en dias habiles
    private LocalDate fechaEntregaEstimada;

    // de donde salieron los feriados usados en el calculo (NAGER_DATE, ARGENTINA_DATOS, CACHE...)
    @Column(length = 40)
    private String fuenteCalendario;

    // false si no se pudieron obtener los feriados y solo se descontaron fines de semana
    private Boolean calendarioConfiable;

    private LocalDateTime fechaCancelacion;

    @Column(length = 255)
    private String motivoCancelacion;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 60)
    private String creadoPor;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleOrden> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero")
    private List<Recepcion> recepciones = new ArrayList<>();

    // bloqueo optimista: dos recepciones simultaneas de la misma orden no pueden pisarse
    @Version
    private Long version;

    public void agregarDetalle(DetalleOrden detalle) {
        detalle.setOrden(this);
        detalles.add(detalle);
    }

    public void agregarRecepcion(Recepcion recepcion) {
        recepcion.setOrden(this);
        recepciones.add(recepcion);
    }

    public BigDecimal getTotal() {
        return detalles.stream().map(DetalleOrden::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean estaCompleta() {
        return detalles.stream().allMatch(d -> d.getPendiente().signum() == 0);
    }
}
