package ppai.seguimiento.infrastructure.web;

import ppai.seguimiento.application.port.BolsinNotificationRepository;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.NotificacionInterna;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/seguimiento/notificaciones")
public class BandejaNotificacionesController {

    private final BolsinNotificationRepository notificationRepository;

    public BandejaNotificacionesController(BolsinNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionInterna>> listar(HttpSession session) {
        Object usuarioEnSesion = session.getAttribute("empleadoLogueado");
        if (!(usuarioEnSesion instanceof Empleado empleado)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay un empleado logueado en la sesion.");
        }
        List<NotificacionInterna> notifications = notificationRepository.findNotificationsForDestinationCommission(
                empleado.comisionMedicaAsignada().codigo()
        );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(notifications);
    }
}
