package ar.edu.utn.frc.bda.depositoclick.commons.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Representacion JSON estable de una pagina de resultados.
 * No devolvemos el Page de Spring directamente porque su JSON no es estable
 * (Spring mismo lo desaconseja) y expone detalles internos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponse<T> {
    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
