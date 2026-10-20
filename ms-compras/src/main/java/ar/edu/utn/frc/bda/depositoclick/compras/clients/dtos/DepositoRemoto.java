package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de ms-inventario (/depositos/{id}). Solo los campos que Compras necesita. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositoRemoto {
    private Long id;
    private String codigo;
    private String nombre;
    private String pais;
    private boolean activo;
}
