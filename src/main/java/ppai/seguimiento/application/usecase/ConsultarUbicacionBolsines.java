package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.SeguimientoBolsin;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

@Service
public class ConsultarUbicacionBolsines {

    private final BolsinRepository bolsinRepository;
    private final GpsTrackerPort gpsTracker;

    public ConsultarUbicacionBolsines(BolsinRepository bolsinRepository, GpsTrackerPort gpsTracker) {
        this.bolsinRepository = bolsinRepository;
        this.gpsTracker = gpsTracker;
    }

    public List<SeguimientoBolsin> ejecutar(Empleado empleadoLogueado) {
        return ejecutar(empleadoLogueado, new CancellationToken());
    }

    public List<SeguimientoBolsin> ejecutar(Empleado empleadoLogueado, CancellationToken cancellationToken) {
        String codigoComisionMedicaOrigen = empleadoLogueado.comisionMedicaAsignada().codigo();
        List<Bolsin> bolsines = bolsinRepository.findSentByOriginCommission(codigoComisionMedicaOrigen);
        List<SeguimientoBolsin> seguimientos = new ArrayList<>(bolsines.size());
        for (Bolsin bolsin : bolsines) {
            cancellationToken.throwIfCancelled();
            UbicacionBolsin ubicacion = gpsTracker.obtenerUbicacion(
                    bolsin.numeroBolsin(),
                    codigoComisionMedicaOrigen,
                    cancellationToken
            );
            cancellationToken.throwIfCancelled();
            seguimientos.add(new SeguimientoBolsin(
                    bolsin.numeroBolsin(),
                    bolsin.comisionMedicaDestino().codigo(),
                    bolsin.comisionMedicaDestino().nombre(),
                    bolsin.numeroPrecinto(),
                    bolsin.fecha(),
                    bolsin.peso(),
                    bolsin.estado(),
                    ubicacion.latitud(),
                    ubicacion.longitud(),
                    ubicacion.fechaHoraUltimaActualizacion(),
                    bolsin.notificacionRegistrada()
            ));
        }
        if (!cancellationToken.complete()) {
            throw new CancellationException("La consulta de seguimiento fue cancelada.");
        }
        return List.copyOf(seguimientos);
    }
}
