package ar.edu.utn.frc.bda.depositoclick.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Escaneamos desde "ar.edu.utn.frc.bda.depositoclick" para que tambien se registren
// los componentes del JAR comun (por ejemplo el GlobalExceptionHandler de deposito-commons).
@SpringBootApplication(scanBasePackages = "ar.edu.utn.frc.bda.depositoclick")
public class CatalogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogoApplication.class, args);
    }
}
