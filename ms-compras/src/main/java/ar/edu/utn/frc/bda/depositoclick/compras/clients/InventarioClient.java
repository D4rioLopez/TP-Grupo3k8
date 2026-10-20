package ar.edu.utn.frc.bda.depositoclick.compras.clients;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ErrorRemotoException;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.DepositoRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.IngresoRemotoRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.OperacionRemota;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ResumenReposicionRemoto;
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

/** Cliente de ms-inventario. */
@Component
public class InventarioClient {

    private static final String SERVICIO = "ms-inventario";

    private final RestClient restClient;

    public InventarioClient(@Qualifier("inventarioRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<DepositoRemoto> obtenerDeposito(Long depositoId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/depositos/{id}", depositoId)
                    .retrieve()
                    .body(DepositoRemoto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            throw ErroresRemotos.traducir(SERVICIO, e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar " + SERVICIO + ". Reintente en unos minutos", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("Respuesta inesperada de " + SERVICIO, e);
        }
    }

    /**
     * Registra el ingreso de una recepcion. La operacion es IDEMPOTENTE del lado de inventario
     * (por la referencia), por eso si hay un timeout se puede reintentar sin miedo a duplicar stock.
     */
    public OperacionRemota registrarIngreso(IngresoRemotoRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/movimientos/ingresos")
                    .body(request)
                    .retrieve()
                    .body(OperacionRemota.class);
        } catch (HttpClientErrorException e) {
            throw ErroresRemotos.traducir(SERVICIO, e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("No se pudo confirmar el ingreso en " + SERVICIO
                    + ". La recepcion NO se registro; puede reintentarla sin riesgo de duplicar stock", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("Respuesta inesperada de " + SERVICIO, e);
        }
    }

    public List<ResumenReposicionRemoto> resumenReposicion(Long depositoId, int dias) {
        try {
            List<ResumenReposicionRemoto> r = restClient.get()
                    .uri(b -> b.path("/api/v1/stock/resumen-reposicion")
                            .queryParam("depositoId", depositoId)
                            .queryParam("dias", dias)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ResumenReposicionRemoto>>() { });
            return r == null ? List.of() : r;
        } catch (HttpClientErrorException e) {
            throw ErroresRemotos.traducir(SERVICIO, e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar el stock en " + SERVICIO + ". Reintente en unos minutos", e);
        } catch (RestClientException e) {
            throw new ErrorRemotoException("Respuesta inesperada de " + SERVICIO, e);
        }
    }
}
