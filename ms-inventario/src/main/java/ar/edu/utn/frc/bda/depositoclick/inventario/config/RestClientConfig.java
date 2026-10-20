package ar.edu.utn.frc.bda.depositoclick.inventario.config;

import ar.edu.utn.frc.bda.depositoclick.commons.clients.RestClientSoporte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Llamada interna: con timeouts y propagando el token del usuario (ver deposito-commons)
    @Bean
    RestClient catalogoRestClient(@Value("${app.catalogo.base-url}") String baseUrl) {
        return RestClientSoporte.interno(baseUrl);
    }
}
