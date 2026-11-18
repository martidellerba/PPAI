package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.application.port.ComisionMedicaRepository;
import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.application.port.NotificacionSeguimientoPort;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.NotificacionSeguimiento;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificarSeguimientoBolsin {

    private final BolsinRepository bolsinRepository;
    private final ComisionMedicaRepository comisionRepository;
    private final GpsTrackerPort gpsTracker;
    private final NotificacionSeguimientoPort notificacionPort;

    public NotificarSeguimientoBolsin(
            BolsinRepository bolsinRepository,
            ComisionMedicaRepository comisionRepository,
            GpsTrackerPort gpsTracker,
            NotificacionSeguimientoPort notificacionPort
    ) {
        this.bolsinRepository = bolsinRepository;
        this.comisionRepository = comisionRepository;
        this.gpsTracker = gpsTracker;
        this.notificacionPort = notificacionPort;
    }

    public void ejecutar(Empleado empleadoLogueado, Integer numeroBolsin) {
        String codigoComisionOrigen = empleadoLogueado.comisionMedicaAsignada().codigo();
        Bolsin bolsin = bolsinRepository.findSentByOriginCommission(codigoComisionOrigen).stream()
                .filter(item -> item.numeroBolsin().equals(numeroBolsin))
                .findFirst()
                .orElseThrow(() -> new BolsinNoEncontradoException(numeroBolsin));

        String codigoDestino = bolsin.comisionMedicaDestino().codigo();
        String emailGerente = comisionRepository.findManagerEmailByCommissionCode(codigoDestino)
                .orElseThrow(() -> new GerenteComisionNoConfiguradoException(codigoDestino));
        UbicacionBolsin ubicacion = gpsTracker.obtenerUbicacion(bolsin.numeroBolsin(), codigoComisionOrigen);

        notificacionPort.notificar(new NotificacionSeguimiento(
                bolsin.numeroBolsin(),
                bolsin.numeroPrecinto(),
                bolsin.comisionMedicaOrigen().codigo(),
                bolsin.comisionMedicaOrigen().nombre(),
                codigoDestino,
                bolsin.comisionMedicaDestino().nombre(),
                emailGerente,
                ubicacion.fechaHoraUltimaActualizacion(),
                ubicacion.latitud(),
                ubicacion.longitud()
        ));
    }
}
