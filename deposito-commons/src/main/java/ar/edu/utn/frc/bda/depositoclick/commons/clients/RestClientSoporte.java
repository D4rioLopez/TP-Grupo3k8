package ar.edu.utn.frc.bda.depositoclick.commons.clients;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Arma RestClients con la configuracion que acordamos para toda la solucion:
 * - timeouts SIEMPRE (un servicio colgado no puede colgarnos a nosotros)
 * - log de cada llamada
 * - opcionalmente, propagacion del token del usuario (solo para llamadas internas;
 *   a una API publica externa NUNCA le mandamos nuestro token).
 */
public final class RestClientSoporte {

    private RestClientSoporte() {
    }

    public static RestClient crear(String baseUrl, Duration connectTimeout, Duration readTimeout, boolean propagarToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor(new LogLlamadasInterceptor());
        if (propagarToken) {
            builder.requestInterceptor(new PropagarTokenInterceptor());
        }
        return builder.build();
    }

    /** Cliente para otro microservicio de DepositoClick (dentro de la zona confiable). */
    public static RestClient interno(String baseUrl) {
        return crear(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(5), true);
    }

    /** Cliente para una API publica de Internet. */
    public static RestClient externo(String baseUrl) {
        return crear(baseUrl, Duration.ofSeconds(3), Duration.ofSeconds(5), false);
    }
}
