package ar.edu.utn.frc.bda.depositoclick.compras.config;

import ar.edu.utn.frc.bda.depositoclick.commons.security.KeycloakRolesConverter;
import ar.edu.utn.frc.bda.depositoclick.commons.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permisos de Compras:
 *  - Calendario laboral: cualquier rol (es informacion publica).
 *  - Recepcion de mercaderia: OPERARIO y ADMIN (es quien la tiene adelante en el deposito).
 *  - Consultas de proveedores/ordenes: ADMIN, COMPRAS y OPERARIO (el VENDEDOR no ve costos).
 *  - Todo lo demas (proveedores, ordenes, reposicion): ADMIN y COMPRAS.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/calendario/**").hasAnyRole(Roles.TODOS)
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes-compra/*/recepciones").hasAnyRole(Roles.ADMIN, Roles.OPERARIO)
                .requestMatchers(HttpMethod.GET, "/api/v1/proveedores/**", "/api/v1/ordenes-compra/**")
                    .hasAnyRole(Roles.ADMIN, Roles.COMPRAS, Roles.OPERARIO)
                .requestMatchers("/api/v1/**").hasAnyRole(Roles.ADMIN, Roles.COMPRAS)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(KeycloakRolesConverter.jwtAuthenticationConverter())));
        return http.build();
    }
}
