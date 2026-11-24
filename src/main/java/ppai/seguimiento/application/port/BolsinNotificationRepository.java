package ppai.seguimiento.application.port;

import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.NotificacionInterna;

import java.util.List;
import java.util.Optional;

public interface BolsinNotificationRepository {

    Optional<Bolsin> findSentByOriginCommissionAndNumberForUpdate(
            String codigoComisionMedicaOrigen,
            Integer numeroBolsin
    );

    List<NotificacionInterna> findNotificationsForDestinationCommission(String codigoComisionMedicaDestino);

    void markNotificationRegistered(Integer numeroBolsin);
}
