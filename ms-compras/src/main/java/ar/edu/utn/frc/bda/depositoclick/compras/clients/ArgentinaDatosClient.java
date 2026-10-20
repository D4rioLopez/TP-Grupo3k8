package ar.edu.utn.frc.bda.depositoclick.compras.clients;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoArgentinaDatos;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * API PUBLICA EXTERNA de respaldo: ArgentinaDatos (https://api.argentinadatos.com).
 * Solo tiene feriados de Argentina, pero incluye los "puentes turisticos" que se decretan cada año.
 * Se usa cuando Nager.Date no responde y el deposito es de Argentina.
 */
@Component
public class ArgentinaDatosClient {

    private final RestClient restClient;

    public ArgentinaDatosClient(@Qualifier("argentinaDatosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<FeriadoArgentinaDatos> obtenerFeriados(int anio) {
        try {
            List<FeriadoArgentinaDatos> feriados = restClient.get()
                    .uri("/v1/feriados/{anio}", anio)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FeriadoArgentinaDatos>>() { });
            return feriados == null ? List.of() : feriados;
        } catch (HttpClientErrorException e) {
            throw new ErrorRemotoException("ArgentinaDatos respondio " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("ArgentinaDatos no esta disponible", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("ArgentinaDatos devolvio una respuesta inesperada", e);
        }
    }
}
