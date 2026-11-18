package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataBolsinRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaBolsinRepository implements BolsinRepository {

    private final SpringDataBolsinRepository repository;

    public JpaBolsinRepository(SpringDataBolsinRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Bolsin> findSentByOriginCommission(String codigoComisionMedicaOrigen) {
        return repository.findByEstadoAndCodigoComisionMedicaOrigenIgnoreCase(
                        EstadoBolsin.ENVIADO,
                        codigoComisionMedicaOrigen
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    private Bolsin toDomain(BolsinEntity entity) {
        return new Bolsin(
                entity.getNumeroBolsin(),
                entity.getFecha(),
                entity.getNumeroPrecinto(),
                entity.getPeso(),
                new ComisionMedica(entity.getCodigoComisionMedicaOrigen(), entity.getNombreComisionMedicaOrigen()),
                new ComisionMedica(entity.getCodigoComisionMedicaDestino(), entity.getNombreComisionMedicaDestino()),
                entity.getEstado()
        );
    }
}
