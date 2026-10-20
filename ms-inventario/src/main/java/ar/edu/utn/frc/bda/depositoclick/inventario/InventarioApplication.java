package ar.edu.utn.frc.bda.depositoclick.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Escaneamos desde "ar.edu.utn.frc.bda.depositoclick" para registrar tambien los componentes de deposito-commons
@SpringBootApplication(scanBasePackages = "ar.edu.utn.frc.bda.depositoclick")
public class InventarioApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventarioApplication.class, args);
    }
}
