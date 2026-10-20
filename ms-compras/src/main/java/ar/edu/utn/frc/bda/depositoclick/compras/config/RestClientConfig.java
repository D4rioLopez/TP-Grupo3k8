package ar.edu.utn.frc.bda.depositoclick.compras.config;

import ar.edu.utn.frc.bda.depositoclick.commons.clients.RestClientSoporte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient por cada servicio que consumimos.
 * - internos (catalogo, inventario): propagan el token del usuario
 * - externos (APIs publicas de feriados): NO mandan nuestro token
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient catalogoRestClient(@Value("${app.catalogo.base-url}") String baseUrl) {
        return RestClientSoporte.interno(baseUrl);
    }

    @Bean
    RestClient inventarioRestClient(@Value("${app.inventario.base-url}") String baseUrl) {
        return RestClientSoporte.interno(baseUrl);
    }

    @Bean
    RestClient nagerRestClient(@Value("${app.feriados.nager-url}") String baseUrl) {
        return RestClientSoporte.externo(baseUrl);
    }

    @Bean
    RestClient argentinaDatosRestClient(@Value("${app.feriados.argentinadatos-url}") String baseUrl) {
        return RestClientSoporte.externo(baseUrl);
    }
}
