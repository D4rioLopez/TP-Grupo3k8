package ar.edu.utn.frc.bda.depositoclick.compras.services;

import org.springframework.stereotype.Component;

/**
 * Valida el digito verificador de un CUIT/CUIL (algoritmo modulo 11 de AFIP/ARCA).
 * Se multiplica cada uno de los 10 primeros digitos por 5,4,3,2,7,6,5,4,3,2; el verificador es
 * 11 - (suma % 11), con 11 -> 0. Si da 10 el numero no es valido.
 */
@Component
public class ValidadorCuit {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    public String normalizar(String cuit) {
        return cuit == null ? null : cuit.replace("-", "").trim();
    }

    public boolean esValido(String cuit) {
        String numero = normalizar(cuit);
        if (numero == null || !numero.matches("^\\d{11}$")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (numero.charAt(i) - '0') * PESOS[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        }
        if (verificador == 10) {
            return false;
        }
        return verificador == numero.charAt(10) - '0';
    }
}
