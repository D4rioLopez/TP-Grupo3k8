package ar.edu.utn.frc.bda.depositoclick.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Prueba el Security Filter del gateway. Los microservicios no estan levantados (puertos inexistentes),
 * asi que si la peticion PASA la seguridad, el gateway responde 503 (servicio no disponible).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.gateway.url-catalogo=http://localhost:65011",
        "app.gateway.url-inventario=http://localhost:65012",
        "app.gateway.url-compras=http://localhost:65013"
})
@AutoConfigureWebTestClient
class GatewaySeguridadTest {

    @Autowired
    private WebTestClient client;

    @Test
    void sinTokenDevuelve401() {
        client.get().uri("/api/v1/articulos").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void conTokenPeroSinRolDeDepositoClickDevuelve403() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_OTRA_APP")))
                .get().uri("/api/v1/articulos").exchange().expectStatus().isForbidden();
    }

    @Test
    void vendedorNoPuedeVerReposicion() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                .get().uri("/api/v1/reposicion/sugerencias?depositoId=1").exchange().expectStatus().isForbidden();
    }

    @Test
    void rutaNoExpuestaSeRechaza() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .get().uri("/actuator/env").exchange().expectStatus().isForbidden();
    }

    @Test
    void siPasaLaSeguridadPeroElServicioEstaCaidoDevuelve503() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))
                .get().uri("/api/v1/articulos").exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().exists("X-Request-Id")
                .expectBody().jsonPath("$.mensaje").exists();
    }
}
