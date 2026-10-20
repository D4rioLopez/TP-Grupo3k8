package ar.edu.utn.frc.bda.depositoclick.compras.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proveedores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 11 digitos, sin guiones, con digito verificador valido
    @Column(nullable = false, unique = true, length = 11)
    private String cuit;

    @Column(nullable = false, length = 120)
    private String razonSocial;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String telefono;

    // cuantos DIAS HABILES tarda en entregar desde que se le emite la orden
    @Column(nullable = false)
    private int leadTimeDiasHabiles;

    @Column(nullable = false)
    private boolean activo;
}
