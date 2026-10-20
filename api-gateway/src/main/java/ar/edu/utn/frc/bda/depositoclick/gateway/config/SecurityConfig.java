package ar.edu.utn.frc.bda.depositoclick.gateway.config;

import ar.edu.utn.frc.bda.depositoclick.commons.security.KeycloakRolesConverter;
import ar.edu.utn.frc.bda.depositoclick.commons.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * SECURITY FILTER del Gateway (Spring Security reactivo: el gateway corre sobre WebFlux).
 *
 * Es el PRIMER punto de control de la zona confiable:
 *  - toda peticion a /api/v1/** tiene que traer un JWT valido emitido por Keycloak (firma, emisor, vencimiento)
 *  - el usuario tiene que tener al menos un rol de DepositoClick
 *  - reglas "gruesas" por ruta (ej: la reposicion es solo de ADMIN/COMPRAS)
 *  - cualquier ruta que no conocemos se rechaza (denyAll)
 *
 * Las reglas finas (que motivo de egreso puede usar un VENDEDOR, etc.) quedan en cada microservicio,
 * que ademas vuelve a validar el token (defensa en profundidad). El gateway reenvia el header
 * Authorization tal cual, asi el servicio sabe quien es el usuario.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filtroDeSeguridad(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // documentacion y salud: publicas
                .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**", "/docs/**",
                        "/actuator/health/**").permitAll()
                // reglas gruesas por ruta
                .pathMatchers(HttpMethod.POST, "/api/v1/movimientos/ajustes").hasRole(Roles.ADMIN)
                .pathMatchers("/api/v1/reposicion/**").hasAnyRole(Roles.ADMIN, Roles.COMPRAS)
                .pathMatchers(HttpMethod.GET, "/api/v1/proveedores/**", "/api/v1/ordenes-compra/**")
                    .hasAnyRole(Roles.ADMIN, Roles.COMPRAS, Roles.OPERARIO)
                .pathMatchers(HttpMethod.POST, "/api/v1/ordenes-compra/*/recepciones").hasAnyRole(Roles.ADMIN, Roles.OPERARIO)
                .pathMatchers("/api/v1/proveedores/**", "/api/v1/ordenes-compra/**").hasAnyRole(Roles.ADMIN, Roles.COMPRAS)
                // el resto de la API: cualquier usuario con un rol de DepositoClick
                .pathMatchers("/api/v1/**").hasAnyRole(Roles.TODOS)
                // lo que no esta expuesto, no existe
                .anyExchange().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(KeycloakRolesConverter.jwtAuthenticationConverter()))));
        return http.build();
    }
}
