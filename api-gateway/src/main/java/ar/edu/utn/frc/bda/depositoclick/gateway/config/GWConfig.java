package ar.edu.utn.frc.bda.depositoclick.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mapa de rutas del gateway. El cliente solo conoce http://gateway:8080/api/v1/...
 * y no sabe (ni le importa) que detras hay tres microservicios.
 *
 *   /api/v1/rubros, /categorias, /articulos                      -> ms-catalogo   :8081
 *   /api/v1/depositos, /movimientos, /stock                      -> ms-inventario :8082
 *   /api/v1/proveedores, /ordenes-compra, /reposicion, /calendario -> ms-compras  :8083
 *   /docs/{servicio}  -> /v3/api-docs de cada servicio (para el Swagger UI unificado)
 */
@Configuration
public class GWConfig {

    @Bean
    public RouteLocator configurarRutas(RouteLocatorBuilder builder,
                                        @Value("${app.gateway.url-catalogo}") String uriCatalogo,
                                        @Value("${app.gateway.url-inventario}") String uriInventario,
                                        @Value("${app.gateway.url-compras}") String uriCompras) {
        return builder.routes()
                // Ruteo al Microservicio de Catalogo
                .route("catalogo", r -> r
                        .path("/api/v1/rubros/**", "/api/v1/categorias/**", "/api/v1/articulos/**")
                        .uri(uriCatalogo))
                // Ruteo al Microservicio de Inventario
                .route("inventario", r -> r
                        .path("/api/v1/depositos/**", "/api/v1/movimientos/**", "/api/v1/stock/**")
                        .uri(uriInventario))
                // Ruteo al Microservicio de Compras
                .route("compras", r -> r
                        .path("/api/v1/proveedores/**", "/api/v1/ordenes-compra/**", "/api/v1/reposicion/**",
                                "/api/v1/calendario/**")
                        .uri(uriCompras))
                // Documentacion OpenAPI de cada servicio (reescribe el path)
                .route("docs-catalogo", r -> r.path("/docs/catalogo")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(uriCatalogo))
                .route("docs-inventario", r -> r.path("/docs/inventario")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(uriInventario))
                .route("docs-compras", r -> r.path("/docs/compras")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(uriCompras))
                .build();
    }
}
