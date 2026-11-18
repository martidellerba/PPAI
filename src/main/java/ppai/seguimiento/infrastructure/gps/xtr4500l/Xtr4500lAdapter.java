package ppai.seguimiento.infrastructure.gps.xtr4500l;

import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import ppai.seguimiento.infrastructure.gps.xtr4500l.dto.XtrBolsinLocationItemDto;
import ppai.seguimiento.infrastructure.gps.xtr4500l.dto.XtrBolsinLocationResponseDto;
import org.springframework.stereotype.Component;

@Component
public class Xtr4500lAdapter implements GpsTrackerPort {

    private final Xtr4500lClient adaptee;

    public Xtr4500lAdapter(Xtr4500lClient adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public UbicacionBolsin obtenerUbicacion(Integer numeroBolsin, String codigoComisionMedicaOrigen) {
        return convertirRespuesta(numeroBolsin, adaptee.getBolsinLocation(numeroBolsin, codigoComisionMedicaOrigen));
    }

    @Override
    public UbicacionBolsin obtenerUbicacion(
            Integer numeroBolsin,
            String codigoComisionMedicaOrigen,
            CancellationToken cancellationToken
    ) {
        cancellationToken.throwIfCancelled();
        XtrBolsinLocationResponseDto response = adaptee.getBolsinLocation(
                numeroBolsin,
                codigoComisionMedicaOrigen,
                cancellationToken
        );
        cancellationToken.throwIfCancelled();
        return convertirRespuesta(numeroBolsin, response);
    }

    private UbicacionBolsin convertirRespuesta(Integer numeroBolsin, XtrBolsinLocationResponseDto response) {
        if (response == null || response.items() == null) {
            throw new GpsTrackerException("La API XTR-4500L devolvio una respuesta vacia.");
        }

        XtrBolsinLocationItemDto item = response.items().stream()
                .filter(location -> location != null)
                .filter(location -> numeroBolsin.equals(location.numeroBolsin()))
                .findFirst()
                .orElseThrow(() -> new GpsTrackerException(
                        "La API XTR-4500L no devolvio la ubicacion del bolsin " + numeroBolsin + "."
                ));

        if (item.latitud() == null || item.longitud() == null
                || !Double.isFinite(item.latitud()) || !Double.isFinite(item.longitud())
                || item.latitud() < -90 || item.latitud() > 90
                || item.longitud() < -180 || item.longitud() > 180
                || item.fechaHoraUltimaActualizacion() == null) {
            throw new GpsTrackerException("La API XTR-4500L devolvio datos incompletos o invalidos para el bolsin " + numeroBolsin + ".");
        }

        return new UbicacionBolsin(
                item.numeroBolsin(),
                item.latitud(),
                item.longitud(),
                item.fechaHoraUltimaActualizacion()
        );
    }
}
