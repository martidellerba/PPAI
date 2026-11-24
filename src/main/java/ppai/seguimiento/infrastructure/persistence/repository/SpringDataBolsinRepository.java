package ppai.seguimiento.infrastructure.persistence.repository;

import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SpringDataBolsinRepository extends JpaRepository<BolsinEntity, Integer> {
    List<BolsinEntity> findByEstadoAndCodigoComisionMedicaOrigenIgnoreCase(
            EstadoBolsin estado,
            String codigoComisionMedicaOrigen
    );

    List<BolsinEntity> findByNotificacionRegistradaTrueAndCodigoComisionMedicaDestinoIgnoreCaseOrderByFechaHoraNotificacionDesc(
            String codigoComisionMedicaDestino
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bolsin from BolsinEntity bolsin
            where bolsin.numeroBolsin = :numeroBolsin
              and lower(bolsin.codigoComisionMedicaOrigen) = lower(:codigoComision)
              and bolsin.estado = :estado
            """)
    Optional<BolsinEntity> findSentByOriginCommissionAndNumberForUpdate(
            @Param("codigoComision") String codigoComision,
            @Param("numeroBolsin") Integer numeroBolsin,
            @Param("estado") EstadoBolsin estado
    );
}
