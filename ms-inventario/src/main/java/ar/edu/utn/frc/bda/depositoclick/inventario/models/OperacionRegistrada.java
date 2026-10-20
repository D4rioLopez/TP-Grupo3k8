package ar.edu.utn.frc.bda.depositoclick.inventario.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de cada operacion procesada, identificada por su "referencia" (unica).
 * Es lo que hace idempotentes a los ingresos/egresos: si ms-compras reintenta la recepcion
 * "OC-15-REC-1" porque no le llego la respuesta, no se duplica el stock.
 */
@Entity
@Table(name = "operaciones_registradas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionRegistrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    // INGRESO, EGRESO, TRANSFERENCIA o AJUSTE
    @Column(nullable = false, length = 20)
    private String tipoOperacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MotivoMovimiento motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 60)
    private String usuario;
}
