package ar.edu.utn.frc.bda.depositoclick.commons.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Registra cada llamada saliente: metodo, URI, status y tiempo.
 * (Nunca logueamos el body ni el token para no exponer datos sensibles).
 */
@Slf4j
public class LogLlamadasInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long t0 = System.currentTimeMillis();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("-> {} {} | {} | {}ms", request.getMethod(), request.getURI(),
                    response.getStatusCode().value(), System.currentTimeMillis() - t0);
            return response;
        } catch (IOException e) {
            log.warn("-> {} {} | ERROR {} | {}ms", request.getMethod(), request.getURI(),
                    e.getClass().getSimpleName(), System.currentTimeMillis() - t0);
            throw e;
        }
    }
}
