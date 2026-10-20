package ar.edu.utn.frc.bda.depositoclick.commons.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Convierte los roles que Keycloak manda en el JWT al formato que espera Spring Security.
 *
 * Keycloak los manda asi:   "realm_access": { "roles": ["ADMIN", "offline_access", ...] }
 * Spring Security espera:   GrantedAuthority "ROLE_ADMIN" (por eso hasRole("ADMIN") funciona)
 *
 * Esta clase la usan los 3 microservicios (Spring MVC) y tambien el API Gateway (WebFlux),
 * por eso no depende de nada de servlets: solo de Spring Security.
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String CLAIM_USUARIO = "preferred_username";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()))
                .toList();
    }

    /**
     * Converter completo para usar en .oauth2ResourceServer(...).
     * Ademas de los roles, hace que authentication.getName() devuelva el nombre de usuario
     * (preferred_username) en lugar del "sub" (que es un UUID poco legible para auditoria).
     */
    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        converter.setPrincipalClaimName(CLAIM_USUARIO);
        return converter;
    }
}
