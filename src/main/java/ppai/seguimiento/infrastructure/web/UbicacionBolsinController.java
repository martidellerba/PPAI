package ppai.seguimiento.infrastructure.web;

import ppai.seguimiento.application.usecase.ConsultarUbicacionBolsines;
import ppai.seguimiento.application.usecase.ConsultaCancellationRegistry;
import ppai.seguimiento.application.usecase.ConsultaNoActivaException;
import ppai.seguimiento.application.usecase.BolsinNoEncontradoException;
import ppai.seguimiento.application.usecase.NotificarSeguimientoBolsin;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.SeguimientoBolsin;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import ppai.seguimiento.application.port.CancellationToken;

@RestController
@RequestMapping("/api/seguimiento")
public class UbicacionBolsinController {

    private final ConsultarUbicacionBolsines consultarUbicacionBolsines;
    private final NotificarSeguimientoBolsin notificarSeguimientoBolsin;
    private final ConsultaCancellationRegistry cancellationRegistry;

    public UbicacionBolsinController(
            ConsultarUbicacionBolsines consultarUbicacionBolsines,
            NotificarSeguimientoBolsin notificarSeguimientoBolsin,
            ConsultaCancellationRegistry cancellationRegistry
    ) {
        this.consultarUbicacionBolsines = consultarUbicacionBolsines;
        this.notificarSeguimientoBolsin = notificarSeguimientoBolsin;
        this.cancellationRegistry = cancellationRegistry;
    }

    @PostMapping("/consultas/{consultaId}")
    public ResponseEntity<Void> registrarConsulta(@PathVariable UUID consultaId, HttpSession session) {
        requireEmployee(session);
        try {
            cancellationRegistry.register(session.getId(), consultaId);
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bolsines")
    public List<SeguimientoBolsin> consultarUbicacionBolsines(
            @RequestParam(required = false) UUID consultaId,
            HttpSession session
    ) {
        Empleado empleadoLogueado = requireEmployee(session);
        if (consultaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el identificador de la consulta.");
        }
        CancellationToken cancellationToken = cancellationRegistry.get(session.getId(), consultaId);
        try {
            return consultarUbicacionBolsines.ejecutar(empleadoLogueado, cancellationToken);
        } finally {
            cancellationRegistry.finish(session.getId(), consultaId);
        }
    }

    @PostMapping("/consultas/{consultaId}/cancelacion")
    public ResponseEntity<Void> cancelarConsulta(@PathVariable UUID consultaId, HttpSession session) {
        requireEmployee(session);
        return cancellationRegistry.cancel(session.getId(), consultaId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/sesion")
    public ResponseEntity<Empleado> consultarSesion(HttpSession session) {
        Object usuarioEnSesion = session.getAttribute("empleadoLogueado");
        return usuarioEnSesion instanceof Empleado empleado
                ? ResponseEntity.ok(empleado)
                : ResponseEntity.noContent().build();
    }

    @PostMapping("/bolsines/{numeroBolsin}/notificacion")
    public ResponseEntity<Void> notificarSeguimiento(
            @PathVariable Integer numeroBolsin,
            HttpSession session
    ) {
        Object usuarioEnSesion = session.getAttribute("empleadoLogueado");
        if (!(usuarioEnSesion instanceof Empleado empleadoLogueado)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay un empleado logueado en la sesion.");
        }
        notificarSeguimientoBolsin.ejecutar(empleadoLogueado, numeroBolsin);
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(BolsinNoEncontradoException.class)
    public ResponseEntity<String> handleBolsinNoEncontrado(BolsinNoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler({CancellationException.class, ConsultaNoActivaException.class})
    public ResponseEntity<String> handleConsultaCancelada(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    private Empleado requireEmployee(HttpSession session) {
        Object usuarioEnSesion = session.getAttribute("empleadoLogueado");
        if (!(usuarioEnSesion instanceof Empleado empleadoLogueado)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay un empleado logueado en la sesion.");
        }
        return empleadoLogueado;
    }

    @ExceptionHandler(ppai.seguimiento.application.usecase.GpsTrackerException.class)
    public ResponseEntity<String> handleGpsTrackerError(
            ppai.seguimiento.application.usecase.GpsTrackerException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(exception.getMessage());
    }

    @ExceptionHandler(ppai.seguimiento.application.usecase.GerenteComisionNoConfiguradoException.class)
    public ResponseEntity<String> handleGerenteNoConfigurado(
            ppai.seguimiento.application.usecase.GerenteComisionNoConfiguradoException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
