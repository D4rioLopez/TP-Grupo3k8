package ar.edu.utn.frc.bda.depositoclick.gateway.config;

import ar.edu.utn.frc.bda.depositoclick.commons.dtos.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/**
 * Errores que se producen EN el gateway (no en los microservicios): servicio caido, timeout, ruta inexistente.
 * Se responden con el mismo formato JSON (ErrorResponse de deposito-commons) que usan los microservicios,
 * asi el cliente siempre recibe errores con la misma forma.
 * Order(-2) para ganarle al manejador por defecto de Spring Boot (-1).
 */
@Slf4j
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatusCode status;
        String mensaje;
        if (contiene(ex, ConnectException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            mensaje = "El servicio que atiende " + exchange.getRequest().getURI().getPath() + " no esta disponible. Reintente en unos minutos";
        } else if (contiene(ex, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            mensaje = "El servicio no respondio a tiempo";
        } else if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode();
            mensaje = status.value() == 404 ? "No existe el recurso " + exchange.getRequest().getURI().getPath()
                    : (rse.getReason() != null ? rse.getReason() : "Error en el gateway");
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            mensaje = "Error interno del gateway";
        }
        if (status.is5xxServerError()) {
            log.warn("{} {} -> {}: {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(),
                    status.value(), ex.toString());
        }

        HttpStatus resuelto = HttpStatus.resolve(status.value());
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(resuelto != null ? resuelto.getReasonPhrase() : String.valueOf(status.value()))
                .mensaje(mensaje)
                .path(exchange.getRequest().getURI().getPath())
                .build();
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private boolean contiene(Throwable ex, Class<? extends Throwable> tipo) {
        Throwable actual = ex;
        while (actual != null) {
            if (tipo.isInstance(actual)) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
