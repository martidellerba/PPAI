package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.application.port.ComisionMedicaRepository;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataComisionMedicaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaComisionMedicaDirectory implements ComisionMedicaRepository {

    private final SpringDataComisionMedicaRepository repository;

    public JpaComisionMedicaDirectory(SpringDataComisionMedicaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> findManagerEmailByCommissionCode(String codigoComisionMedica) {
        return repository.findById(codigoComisionMedica)
                .map(comision -> comision.getEmailGerente())
                .filter(email -> email != null && !email.isBlank());
    }
}
