package ar.edu.utn.frc.bda.depositoclick.compras.clients;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.FeriadoNager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * API PUBLICA EXTERNA: Nager.Date (https://date.nager.at), feriados nacionales de +100 paises.
 * Gratuita y sin API key. Es la fuente principal del calendario laboral.
 */
@Component
public class NagerDateClient {

    private final RestClient restClient;

    public NagerDateClient(@Qualifier("nagerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @return Optional.empty() si Nager no conoce el pais (responde 404). Es una respuesta valida,
     * no una falla: no tiene sentido reintentar ni usar otra fuente.
     */
    public Optional<List<FeriadoNager>> obtenerFeriados(String pais, int anio) {
        try {
            List<FeriadoNager> feriados = restClient.get()
                    .uri("/api/v3/PublicHolidays/{anio}/{pais}", anio, pais)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FeriadoNager>>() { });
            return Optional.of(feriados == null ? List.of() : feriados);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            throw new ErrorRemotoException("Nager.Date respondio " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("Nager.Date no esta disponible", e);
        } catch (RestClientException e) {
            // por ejemplo: devolvio HTML o un JSON con otra forma
            throw new ErrorRemotoException("Nager.Date devolvio una respuesta inesperada", e);
        }
    }
}
