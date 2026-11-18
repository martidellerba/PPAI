package ppai.seguimiento.infrastructure.persistence.repository;

import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataBolsinRepository extends JpaRepository<BolsinEntity, Integer> {
    List<BolsinEntity> findByEstadoAndCodigoComisionMedicaOrigenIgnoreCase(
            EstadoBolsin estado,
            String codigoComisionMedicaOrigen
    );
}
