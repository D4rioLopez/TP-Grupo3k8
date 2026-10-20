package ar.edu.utn.frc.bda.depositoclick.inventario.clients;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cliente HTTP (RestClient) de ms-catalogo.
 * Distingue tres situaciones que el negocio trata distinto:
 *  - 404           -> "el articulo no existe": es una RESPUESTA valida (Optional.empty)
 *  - caido/timeout -> "no pude preguntar": ServicioNoDisponibleException (se puede usar la copia local)
 *  - otra cosa     -> respuesta inesperada: ErrorRemotoException
 */
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<ReglasArticuloRemoto> obtenerReglas(Long articuloId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/articulos/{id}/reglas-stock", articuloId)
                    .retrieve()
                    .body(ReglasArticuloRemoto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar ms-catalogo", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("Respuesta inesperada de ms-catalogo: " + e.getMessage(), e);
        }
    }

    public List<ReglasArticuloRemoto> obtenerReglas(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String lista = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<ReglasArticuloRemoto> respuesta = restClient.get()
                    .uri(b -> b.path("/api/v1/articulos/reglas-stock").queryParam("ids", lista).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ReglasArticuloRemoto>>() { });
            return respuesta == null ? List.of() : respuesta;
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar ms-catalogo", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("Respuesta inesperada de ms-catalogo: " + e.getMessage(), e);
        }
    }
}
