package ar.edu.utn.frc.bda.depositoclick.catalogo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private Long rubroId;
    private String rubroNombre;
    private boolean controlaLote;
    private boolean controlaVencimiento;
    private List<AtributoDTO> atributos;
}
