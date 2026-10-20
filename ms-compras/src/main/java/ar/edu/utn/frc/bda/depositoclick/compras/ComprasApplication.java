package ar.edu.utn.frc.bda.depositoclick.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Escaneamos desde "ar.edu.utn.frc.bda.depositoclick" para registrar tambien los componentes de deposito-commons
@SpringBootApplication(scanBasePackages = "ar.edu.utn.frc.bda.depositoclick")
public class ComprasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComprasApplication.class, args);
    }
}
