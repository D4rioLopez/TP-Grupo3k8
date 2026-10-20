package ar.edu.utn.frc.bda.depositoclick.inventario.config;

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
 * Permisos de Inventario:
 *  - Consultas: cualquier rol.
 *  - Egresos: VENDEDOR (solo VENTA, se controla en el servicio), OPERARIO y ADMIN.
 *  - Ingresos y transferencias: OPERARIO y ADMIN.
 *  - Ajustes por inventario fisico y ABM de depositos: solo ADMIN.
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
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(Roles.TODOS)
                .requestMatchers(HttpMethod.POST, "/api/v1/movimientos/egresos").hasAnyRole(Roles.ADMIN, Roles.OPERARIO, Roles.VENDEDOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/movimientos/ingresos", "/api/v1/movimientos/transferencias")
                    .hasAnyRole(Roles.ADMIN, Roles.OPERARIO)
                .requestMatchers("/api/v1/**").hasRole(Roles.ADMIN)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(KeycloakRolesConverter.jwtAuthenticationConverter())));
        return http.build();
    }
}
