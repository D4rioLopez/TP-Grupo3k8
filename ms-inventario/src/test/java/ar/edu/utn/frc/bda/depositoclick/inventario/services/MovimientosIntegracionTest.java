package ar.edu.utn.frc.bda.depositoclick.inventario.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.inventario.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.inventario.clients.ReglasArticuloRemoto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de las reglas de stock de punta a punta (H2 + seguridad simulada).
 * El catalogo se reemplaza por un mock para poder simular que esta caido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MovimientosIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogoClient catalogoClient;

    private static JwtRequestPostProcessor usuario(String nombre, String rol) {
        // jwt() de spring-security-test no pasa por nuestro converter: el nombre sale del "sub"
        return jwt().jwt(j -> j.subject(nombre).claim("preferred_username", nombre))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private void articuloConVencimiento(long id, String sku) {
        when(catalogoClient.obtenerReglas(eq(id))).thenReturn(Optional.of(new ReglasArticuloRemoto(
                id, sku, "Articulo " + sku, "UNIDAD", false, true, true, BigDecimal.TEN, true)));
    }

    private String ingreso(String referencia, long articuloId, String lote, int cantidad, LocalDate vence) {
        return """
                {"depositoId":1,"motivo":"STOCK_INICIAL","referencia":"%s",
                 "items":[{"articuloId":%d,"cantidad":%d,"lote":"%s","fechaVencimiento":"%s"}]}
                """.formatted(referencia, articuloId, cantidad, lote, vence);
    }

    @Test
    void egresoDespachaPorFefoYNoPermiteNegativo() throws Exception {
        articuloConVencimiento(100L, "LAC-100");
        LocalDate hoy = LocalDate.now();
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-ING-1", 100, "L-TARDE", 10, hoy.plusDays(20)))
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-ING-2", 100, "L-PRONTO", 10, hoy.plusDays(5)))
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated());

        // pide 15: tiene que sacar primero los 10 del lote que vence antes
        mvc.perform(post("/api/v1/movimientos/egresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"VENTA\",\"items\":[{\"articuloId\":100,\"cantidad\":15}]}")
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimientos[0].lote").value("L-PRONTO"))
                .andExpect(jsonPath("$.movimientos[0].cantidad").value(10))
                .andExpect(jsonPath("$.movimientos[1].lote").value("L-TARDE"))
                .andExpect(jsonPath("$.movimientos[1].cantidad").value(5))
                .andExpect(jsonPath("$.usuario").value("vendedor1"));

        // quedan 5: pedir 6 es stock insuficiente
        mvc.perform(post("/api/v1/movimientos/egresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"VENTA\",\"items\":[{\"articuloId\":100,\"cantidad\":6}]}")
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("Stock insuficiente")));

        mvc.perform(get("/api/v1/stock/articulos/100").with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDisponible").value(5))
                .andExpect(jsonPath("$.bajoMinimo").value(true));
    }

    @Test
    void reglasDeLoteVencimientoYUnidad() throws Exception {
        articuloConVencimiento(101L, "LAC-101");
        // sin lote
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"STOCK_INICIAL\",\"items\":[{\"articuloId\":101,\"cantidad\":5,\"fechaVencimiento\":\"2099-01-01\"}]}")
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("lote")));
        // vencido
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-ING-VENC", 101, "L1", 5, LocalDate.now().minusDays(1)))
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isUnprocessableEntity());
        // decimales en un articulo por UNIDAD
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"STOCK_INICIAL\",\"items\":[{\"articuloId\":101,\"cantidad\":1.5,\"lote\":\"L1\",\"fechaVencimiento\":\"2099-01-01\"}]}")
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("entera")));
    }

    @Test
    void laMismaReferenciaNoSeAplicaDosVeces() throws Exception {
        articuloConVencimiento(102L, "LAC-102");
        String body = ingreso("OC-99-REC-1", 102, "L9", 7, LocalDate.now().plusDays(30)).replace("STOCK_INICIAL", "COMPRA");
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetida").value(true));
        mvc.perform(get("/api/v1/stock/articulos/102").with(usuario("operario1", "OPERARIO")))
                .andExpect(jsonPath("$.stockTotal").value(7));
    }

    @Test
    void vendedorSoloPuedeVender() throws Exception {
        mvc.perform(post("/api/v1/movimientos/egresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"ROTURA\",\"items\":[{\"articuloId\":100,\"cantidad\":1}]}")
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-X", 100, "L", 1, LocalDate.now().plusDays(3)))
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogoCaidoUsaCopiaLocalOSinCopiaDevuelve503() throws Exception {
        // primero se consulta OK (queda la copia local)
        articuloConVencimiento(103L, "LAC-103");
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-ING-103", 103, "L1", 5, LocalDate.now().plusDays(30)))
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated());

        // se cae el catalogo
        when(catalogoClient.obtenerReglas(eq(103L))).thenThrow(new ServicioNoDisponibleException("caido"));
        when(catalogoClient.obtenerReglas(eq(104L))).thenThrow(new ServicioNoDisponibleException("caido"));

        mvc.perform(post("/api/v1/movimientos/egresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"VENTA\",\"items\":[{\"articuloId\":103,\"cantidad\":1}]}")
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advertencias[0]", containsString("copia local")));

        mvc.perform(post("/api/v1/movimientos/egresos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoId\":1,\"motivo\":\"VENTA\",\"items\":[{\"articuloId\":104,\"cantidad\":1}]}")
                        .with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void transferenciaConservaLoteYVencimiento() throws Exception {
        articuloConVencimiento(105L, "LAC-105");
        LocalDate vence = LocalDate.now().plusDays(40);
        mvc.perform(post("/api/v1/movimientos/ingresos").contentType(MediaType.APPLICATION_JSON)
                        .content(ingreso("TEST-ING-105", 105, "LT", 8, vence))
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/movimientos/transferencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositoOrigenId\":1,\"depositoDestinoId\":2,\"items\":[{\"articuloId\":105,\"cantidad\":3}]}")
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimientos[1].tipo").value("TRANSFERENCIA_ENTRADA"))
                .andExpect(jsonPath("$.movimientos[1].lote").value("LT"))
                .andExpect(jsonPath("$.movimientos[1].fechaVencimiento").value(vence.toString()));
    }
}
