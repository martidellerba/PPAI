package ppai.seguimiento.infrastructure.persistence;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.application.port.BolsinNotificationRepository;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.domain.model.NotificacionInterna;
import ppai.seguimiento.infrastructure.persistence.entity.BolsinEntity;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataBolsinRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaBolsinRepository implements BolsinRepository, BolsinNotificationRepository {

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

    @Override
    public Optional<Bolsin> findSentByOriginCommissionAndNumberForUpdate(
            String codigoComisionMedicaOrigen,
            Integer numeroBolsin
    ) {
        return repository.findSentByOriginCommissionAndNumberForUpdate(
                        codigoComisionMedicaOrigen,
                        numeroBolsin,
                        EstadoBolsin.ENVIADO
                )
                .map(this::toDomain);
    }

    @Override
    public void markNotificationRegistered(Integer numeroBolsin) {
        repository.findById(numeroBolsin).orElseThrow().marcarNotificacionRegistrada();
    }

    @Override
    public List<NotificacionInterna> findNotificationsForDestinationCommission(String codigoComisionMedicaDestino) {
        return repository
                .findByNotificacionRegistradaTrueAndCodigoComisionMedicaDestinoIgnoreCaseOrderByFechaHoraNotificacionDesc(
                        codigoComisionMedicaDestino
                )
                .stream()
                .map(entity -> new NotificacionInterna(
                        entity.getNumeroBolsin(),
                        entity.getNumeroPrecinto(),
                        entity.getCodigoComisionMedicaOrigen(),
                        entity.getNombreComisionMedicaOrigen(),
                        entity.getCodigoComisionMedicaDestino(),
                        entity.getNombreComisionMedicaDestino(),
                        entity.getFechaHoraNotificacion() == null
                                ? entity.getFecha().atStartOfDay()
                                : entity.getFechaHoraNotificacion()
                ))
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
                entity.getEstado(),
                entity.isNotificacionRegistrada()
        );
    }
}
