package ppai.seguimiento.infrastructure.persistence.repository;

import ppai.seguimiento.infrastructure.persistence.entity.ComisionMedicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataComisionMedicaRepository extends JpaRepository<ComisionMedicaEntity, String> {
}
