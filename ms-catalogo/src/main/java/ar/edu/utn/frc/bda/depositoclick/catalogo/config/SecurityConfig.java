package ar.edu.utn.frc.bda.depositoclick.catalogo.config;

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
 * El catalogo es un Resource Server: valida el JWT que emite Keycloak (firma, emisor, vencimiento)
 * aunque la peticion ya haya pasado por el Gateway (defensa en profundidad: si alguien llegara
 * directo al servicio sin pasar por el gateway, igual necesita un token valido).
 *
 * Permisos:
 *  - Consultar el catalogo: cualquier usuario autenticado de DepositoClick.
 *  - Modificar el catalogo (rubros, categorias, articulos): solo ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // API stateless con tokens: no hay cookies de sesion que proteger
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(Roles.TODOS)
                .requestMatchers("/api/v1/**").hasRole(Roles.ADMIN)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(KeycloakRolesConverter.jwtAuthenticationConverter())));
        return http.build();
    }
}
