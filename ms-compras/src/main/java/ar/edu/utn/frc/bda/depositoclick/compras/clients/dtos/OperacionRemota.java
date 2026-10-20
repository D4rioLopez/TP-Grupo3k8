package ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta de ms-inventario al registrar el ingreso. Solo leemos lo que nos interesa. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperacionRemota {
    private String referencia;
    private boolean repetida;
    private List<String> advertencias;
}
