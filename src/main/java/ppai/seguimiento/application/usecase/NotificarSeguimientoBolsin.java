package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinNotificationRepository;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.Empleado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificarSeguimientoBolsin {

    private final BolsinNotificationRepository notificationRepository;

    public NotificarSeguimientoBolsin(
            BolsinNotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Resultado ejecutar(Empleado empleadoLogueado, Integer numeroBolsin) {
        String codigoComisionOrigen = empleadoLogueado.comisionMedicaAsignada().codigo();
        Bolsin bolsin = notificationRepository.findSentByOriginCommissionAndNumberForUpdate(
                        codigoComisionOrigen,
                        numeroBolsin
                )
                .orElseThrow(() -> new BolsinNoEncontradoException(numeroBolsin));

        if (bolsin.notificacionRegistrada()) {
            return new Resultado(
                    false,
                    bolsin.comisionMedicaDestino().codigo(),
                    bolsin.comisionMedicaDestino().nombre(),
                    true
            );
        }

        notificationRepository.markNotificationRegistered(numeroBolsin);
        return new Resultado(
                true,
                bolsin.comisionMedicaDestino().codigo(),
                bolsin.comisionMedicaDestino().nombre(),
                false
        );
    }

    public record Resultado(
            boolean registradaAhora,
            String codigoComisionDestino,
            String comisionMedicaDestino,
            boolean yaRegistrada
    ) {
    }
}
