package ar.edu.utn.frc.bda.depositoclick.compras.services;

import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.ArgentinaDatosClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.CatalogoClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.InventarioClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.NagerDateClient;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.DepositoRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.IngresoRemotoRequest;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.OperacionRemota;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ReglasArticuloRemoto;
import ar.edu.utn.frc.bda.depositoclick.compras.clients.dtos.ResumenReposicionRemoto;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida de una orden de compra de punta a punta, con los otros microservicios y las APIs
 * de feriados reemplazados por mocks (asi se puede simular que inventario esta caido).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrdenCompraIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogoClient catalogoClient;
    @MockitoBean
    private InventarioClient inventarioClient;
    @MockitoBean
    private NagerDateClient nagerClient;
    @MockitoBean
    private ArgentinaDatosClient argentinaDatosClient;

    private static JwtRequestPostProcessor usuario(String nombre, String rol) {
        return jwt().jwt(j -> j.subject(nombre)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private final ReglasArticuloRemoto leche = new ReglasArticuloRemoto(1L, "LAC-001", "Leche entera 1L", "UNIDAD",
            false, true, true, new BigDecimal("24"), true);

    @BeforeEach
    void setUp() {
        reset(inventarioClient);
        when(catalogoClient.obtenerReglas(1L)).thenReturn(Optional.of(leche));
        when(catalogoClient.obtenerReglas(anyCollection())).thenReturn(List.of(leche));
        when(inventarioClient.obtenerDeposito(1L)).thenReturn(Optional.of(new DepositoRemoto(1L, "DEP-CBA", "Central", "AR", true)));
        when(nagerClient.obtenerFeriados(anyString(), anyInt())).thenReturn(Optional.empty());
    }

    private Long crearYEmitir() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/ordenes-compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":1,\"depositoDestinoId\":1,\"items\":[{\"articuloId\":1,\"cantidad\":48}]}")
                        .with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(950.0))
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/emision").with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.fechaEntregaEstimada").exists());
        return id;
    }

    private String recepcion(int cantidad) {
        return "{\"items\":[{\"articuloId\":1,\"cantidad\":" + cantidad + ",\"lote\":\"L1\",\"fechaVencimiento\":\"2099-12-31\"}]}";
    }

    @Test
    void recepcionParcialYTotalIngresaStockEnInventario() throws Exception {
        Long id = crearYEmitir();
        when(inventarioClient.registrarIngreso(any())).thenReturn(new OperacionRemota("x", false, null));

        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/recepciones").contentType(MediaType.APPLICATION_JSON)
                        .content(recepcion(20)).with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECIBIDA_PARCIAL"))
                .andExpect(jsonPath("$.detalles[0].pendiente").value(28));

        ArgumentCaptor<IngresoRemotoRequest> captor = ArgumentCaptor.forClass(IngresoRemotoRequest.class);
        verify(inventarioClient).registrarIngreso(captor.capture());
        assertEquals("OC-" + id + "-REC-1", captor.getValue().getReferencia());
        assertEquals("COMPRA", captor.getValue().getMotivo());

        // mas de lo pendiente -> 422
        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/recepciones").contentType(MediaType.APPLICATION_JSON)
                        .content(recepcion(29)).with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/recepciones").contentType(MediaType.APPLICATION_JSON)
                        .content(recepcion(28)).with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.recepciones.length()").value(2));

        // recibida no se puede cancelar
        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}").with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isConflict());
    }

    @Test
    void siInventarioNoRespondeNoSeRegistraNada() throws Exception {
        Long id = crearYEmitir();
        when(inventarioClient.registrarIngreso(any())).thenThrow(new ServicioNoDisponibleException("No se pudo confirmar el ingreso"));

        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/recepciones").contentType(MediaType.APPLICATION_JSON)
                        .content(recepcion(10)).with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isServiceUnavailable());

        mvc.perform(get("/api/v1/ordenes-compra/" + id).with(usuario("operario1", "OPERARIO")))
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.recepciones.length()").value(0));
    }

    @Test
    void fechaSolicitadaEnFinDeSemanaSeRechaza() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/ordenes-compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":1,\"depositoDestinoId\":1,\"fechaEntregaSolicitada\":\"2099-01-03\","
                                + "\"items\":[{\"articuloId\":1,\"cantidad\":10}]}")
                        .with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        // 2099-01-03 es sabado
        mvc.perform(post("/api/v1/ordenes-compra/" + id + "/emision").with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("sabado")));
    }

    @Test
    void articuloQueElProveedorNoVende() throws Exception {
        when(catalogoClient.obtenerReglas(6L)).thenReturn(Optional.of(new ReglasArticuloRemoto(6L, "FER-001", "Tornillo",
                "KILOGRAMO", true, false, false, BigDecimal.ONE, true)));
        mvc.perform(post("/api/v1/ordenes-compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":1,\"depositoDestinoId\":1,\"items\":[{\"articuloId\":6,\"cantidad\":2.5}]}")
                        .with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("no tiene asociado")));
    }

    @Test
    void operarioNoPuedeCrearOrdenesYVendedorNoVeCompras() throws Exception {
        mvc.perform(post("/api/v1/ordenes-compra").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":1,\"depositoDestinoId\":1,\"items\":[{\"articuloId\":1,\"cantidad\":1}]}")
                        .with(usuario("operario1", "OPERARIO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/proveedores").with(usuario("vendedor1", "VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sugerenciaDeReposicionConsideraConsumoYLoPendiente() throws Exception {
        // 10 en stock, se venden 5 por dia, minimo 24, proveedor preferido tarda 2 dias habiles
        when(inventarioClient.resumenReposicion(eq(1L), anyInt())).thenReturn(List.of(
                new ResumenReposicionRemoto(1L, "LAC-001", new BigDecimal("10"), new BigDecimal("5"))));

        mvc.perform(get("/api/v1/reposicion/sugerencias?depositoId=1&diasCobertura=10").with(usuario("compras1", "COMPRAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugerencias[0].sku").value("LAC-001"))
                .andExpect(jsonPath("$.sugerencias[0].proveedorRazonSocial").value("Distribuidora Lactea del Centro SA"))
                .andExpect(jsonPath("$.sugerencias[0].cantidadSugerida").exists());
    }

    @Test
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/v1/ordenes-compra")).andExpect(status().isUnauthorized());
        verify(inventarioClient, never()).obtenerDeposito(anyLong());
    }
}
