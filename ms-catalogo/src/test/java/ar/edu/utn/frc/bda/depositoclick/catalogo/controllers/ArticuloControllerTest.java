package ar.edu.utn.frc.bda.depositoclick.catalogo.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta dentro del servicio (H2 + datos iniciales + seguridad con JWT simulado).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticuloControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String REMERA_NUEVA = """
            {"sku":"IND-099","nombre":"Remera roja S","categoriaId":3,"unidadMedida":"UNIDAD",
             "stockMinimo":5,"atributos":{"talle":"S","color":"Rojo"}}
            """;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/articulos")).andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorPuedeConsultar() throws Exception {
        mvc.perform(get("/api/v1/articulos/sku/LAC-001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controlaVencimiento").value(true));
    }

    @Test
    void vendedorNoPuedeCrear() throws Exception {
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON).content(REMERA_NUEVA)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaYValidaAtributos() throws Exception {
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON).content(REMERA_NUEVA)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("IND-099"));

        // talle inexistente -> 422
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON)
                        .content(REMERA_NUEVA.replace("IND-099", "IND-098").replace("\"S\"", "\"XXL\""))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isUnprocessableEntity());

        // SKU repetido -> 409
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON).content(REMERA_NUEVA)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validacionDeEntradaDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"x\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }

    @Test
    void reglasEnLote() throws Exception {
        mvc.perform(get("/api/v1/articulos/reglas-stock?ids=1,6,999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COMPRAS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
