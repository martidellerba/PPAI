package ppai.seguimiento.application.port;

import ppai.seguimiento.domain.model.NotificacionSeguimiento;

public interface NotificacionSeguimientoPort {
    void notificar(NotificacionSeguimiento notificacion);
}
