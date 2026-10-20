package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.models.AtributoCategoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.TipoAtributo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.ValorAtributo;
import ar.edu.utn.frc.bda.depositoclick.commons.exceptions.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadoresTest {

    private final ValidadorCodigoBarras validadorCodigo = new ValidadorCodigoBarras();
    private final ValidadorAtributos validadorAtributos = new ValidadorAtributos();

    @Test
    void codigosDeBarrasValidos() {
        assertTrue(validadorCodigo.esValido("7790001000019")); // EAN-13
        assertTrue(validadorCodigo.esValido("4006381333931")); // EAN-13 real
        assertTrue(validadorCodigo.esValido("96385074"));      // EAN-8
        assertTrue(validadorCodigo.esValido("036000291452"));  // UPC-A
    }

    @Test
    void codigosDeBarrasInvalidos() {
        assertFalse(validadorCodigo.esValido("7790001000014"));
        assertFalse(validadorCodigo.esValido("123"));
        assertFalse(validadorCodigo.esValido("abcdefghijklm"));
    }

    private Categoria remeras() {
        Categoria c = new Categoria();
        c.setNombre("Remeras");
        c.agregarAtributo(new AtributoCategoria("talle", TipoAtributo.OPCION, true, "S,M,L"));
        c.agregarAtributo(new AtributoCategoria("peso", TipoAtributo.NUMERO, false, null));
        return c;
    }

    @Test
    void atributosValidosSeNormalizan() {
        List<ValorAtributo> valores = validadorAtributos.validar(remeras(), Map.of("talle", "m", "peso", "0.250"));
        assertEquals(2, valores.size());
        assertEquals("M", valores.stream().filter(v -> v.getAtributo().getNombre().equals("talle")).findFirst().get().getValor());
        assertEquals("0.25", valores.stream().filter(v -> v.getAtributo().getNombre().equals("peso")).findFirst().get().getValor());
    }

    @Test
    void faltaObligatorio() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> validadorAtributos.validar(remeras(), Map.of("peso", "1")));
        assertTrue(ex.getMessage().contains("talle"));
    }

    @Test
    void opcionInexistenteYAtributoDesconocido() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> validadorAtributos.validar(remeras(), Map.of("talle", "XXL", "color", "rojo")));
        assertTrue(ex.getMessage().contains("XXL"));
        assertTrue(ex.getMessage().contains("color"));
    }

    @Test
    void numeroInvalido() {
        assertThrows(ReglaNegocioException.class,
                () -> validadorAtributos.validar(remeras(), Map.of("talle", "S", "peso", "mucho")));
    }
}
