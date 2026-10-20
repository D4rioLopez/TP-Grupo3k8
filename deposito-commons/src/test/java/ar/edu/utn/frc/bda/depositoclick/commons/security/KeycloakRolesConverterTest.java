package ar.edu.utn.frc.bda.depositoclick.commons.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakRolesConverterTest {

    private Jwt jwtCon(Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("token").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(b::claim);
        return b.build();
    }

    @Test
    void convierteRolesDeRealmConPrefijo() {
        Jwt jwt = jwtCon(Map.of("preferred_username", "admin1",
                "realm_access", Map.of("roles", List.of("admin", "offline_access"))));

        Collection<GrantedAuthority> roles = new KeycloakRolesConverter().convert(jwt);

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void sinClaimDeRolesDevuelveVacio() {
        Jwt jwt = jwtCon(Map.of("preferred_username", "x"));
        assertTrue(new KeycloakRolesConverter().convert(jwt).isEmpty());
    }

    @Test
    void elNombreDelPrincipalEsElUsuario() {
        Jwt jwt = jwtCon(Map.of("preferred_username", "operario1",
                "realm_access", Map.of("roles", List.of("OPERARIO"))));
        assertEquals("operario1", KeycloakRolesConverter.jwtAuthenticationConverter().convert(jwt).getName());
    }
}
