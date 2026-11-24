package ppai.seguimiento.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ppai.seguimiento.application.usecase.AutenticarEmpleado;
import ppai.seguimiento.application.usecase.ConsultaCancellationRegistry;
import ppai.seguimiento.application.usecase.CredencialesInvalidasException;
import ppai.seguimiento.domain.model.Empleado;

@RestController
@RequestMapping("/api/sesion")
public class SessionController {

    private final AutenticarEmpleado autenticarEmpleado;
    private final ConsultaCancellationRegistry cancellationRegistry;

    public SessionController(
            AutenticarEmpleado autenticarEmpleado,
            ConsultaCancellationRegistry cancellationRegistry
    ) {
        this.autenticarEmpleado = autenticarEmpleado;
        this.cancellationRegistry = cancellationRegistry;
    }

    @PostMapping
    public ResponseEntity<Empleado> iniciarSesion(
            @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpSession session
    ) {
        Empleado empleado = autenticarEmpleado.ejecutar(requestBody.email(), requestBody.password());
        request.changeSessionId();
        session.setAttribute("empleadoLogueado", empleado);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(empleado);
    }

    @DeleteMapping
    public ResponseEntity<Void> cerrarSesion(HttpSession session) {
        cancellationRegistry.cancelAll(session.getId());
        session.invalidate();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<String> handleCredencialesInvalidas(CredencialesInvalidasException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(CacheControl.noStore())
                .body(exception.getMessage());
    }

    public record LoginRequest(String email, String password) {
    }
}
