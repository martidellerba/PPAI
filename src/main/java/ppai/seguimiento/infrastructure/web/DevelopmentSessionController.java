package ppai.seguimiento.infrastructure.web;

import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.application.usecase.ConsultaCancellationRegistry;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevelopmentSessionController {

    private final ConsultaCancellationRegistry cancellationRegistry;

    public DevelopmentSessionController(ConsultaCancellationRegistry cancellationRegistry) {
        this.cancellationRegistry = cancellationRegistry;
    }

    @PostMapping("/sesion")
    public Empleado crearSesionDePrueba(HttpSession session) {
        Empleado empleado = new Empleado(
                "Ana",
                "Perez",
                "ana@example.com",
                new ComisionMedica("CM-CBA-01", "Cordoba")
        );
        session.setAttribute("empleadoLogueado", empleado);
        return empleado;
    }

    @DeleteMapping("/sesion")
    public ResponseEntity<Void> cerrarSesionDePrueba(HttpSession session) {
        cancellationRegistry.cancelAll(session.getId());
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
