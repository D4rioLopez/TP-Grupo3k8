package ar.edu.utn.frc.bda.depositoclick.gateway.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro global (pre + post) que se aplica a todas las rutas:
 *  - PRE:  agrega un X-Request-Id a la peticion que va al microservicio (para seguirla en los logs)
 *  - POST: agrega el mismo X-Request-Id a la respuesta y registra metodo, path, usuario, status y tiempo.
 * Al ser el unico punto de entrada, es el lugar natural para centralizar esta traza.
 */
@Slf4j
@Component
public class TrazaPeticionesFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString();
        }
        final String id = requestId;
        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, id).build();
        exchange.getResponse().getHeaders().set(HEADER, id);

        return exchange.getPrincipal()
                .map(p -> p.getName())
                .defaultIfEmpty("anonimo")
                .flatMap(usuario -> chain.filter(exchange.mutate().request(request).build())
                        .doFinally(signal -> log.info("[{}] {} {} | usuario={} | {} | {}ms", id,
                                request.getMethod(), request.getURI().getPath(), usuario,
                                exchange.getResponse().getStatusCode(), System.currentTimeMillis() - inicio)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
