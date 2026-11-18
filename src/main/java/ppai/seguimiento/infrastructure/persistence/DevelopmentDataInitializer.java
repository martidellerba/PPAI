package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataBolsinRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
public class DevelopmentDataInitializer implements CommandLineRunner {

    private final SpringDataBolsinRepository repository;

    public DevelopmentDataInitializer(SpringDataBolsinRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.existsById(101)) {
            repository.save(new BolsinEntity(
                    101,
                    LocalDate.of(2026, 3, 7),
                    "PREC-101",
                    2.5,
                    "CM-CBA-01",
                    "Cordoba",
                    "CM-ROS-02",
                    "Rosario",
                    EstadoBolsin.ENVIADO
            ));
        }
        if (!repository.existsById(102)) {
            repository.save(new BolsinEntity(
                    102,
                    LocalDate.of(2026, 3, 7),
                    "PREC-102",
                    1.8,
                    "CM-ROS-02",
                    "Rosario",
                    "CM-CBA-01",
                    "Cordoba",
                    EstadoBolsin.ENVIADO
            ));
        }
    }
}
