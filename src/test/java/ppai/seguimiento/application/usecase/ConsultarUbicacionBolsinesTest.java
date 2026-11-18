package ppai.seguimiento.application.usecase;

import ppai.seguimiento.application.port.BolsinRepository;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.domain.model.Bolsin;
import ppai.seguimiento.domain.model.ComisionMedica;
import ppai.seguimiento.domain.model.Empleado;
import ppai.seguimiento.domain.model.EstadoBolsin;
import ppai.seguimiento.domain.model.SeguimientoBolsin;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarUbicacionBolsinesTest {

    @Test
    void usaLaComisionDelEmpleadoLogueadoParaConsultarElGps() {
        String codigoComisionDelEmpleado = "CM-CBA-01";
        AtomicReference<String> codigoConsultadoEnRepositorio = new AtomicReference<>();
        AtomicReference<String> codigoEnviadoAlGps = new AtomicReference<>();
        ComisionMedica comisionDistintaEnBolsin = new ComisionMedica("CM-OTRA-01", "Otra");

        BolsinRepository repository = codigo -> {
            codigoConsultadoEnRepositorio.set(codigo);
            return List.of(new Bolsin(
                    101,
                    LocalDate.of(2026, 3, 7),
                    "PREC-101",
                    2.5,
                    comisionDistintaEnBolsin,
                    new ComisionMedica("CM-ROS-02", "Rosario"),
                    EstadoBolsin.ENVIADO
            ));
        };
        GpsTrackerPort gpsTracker = (numero, codigo) -> {
            codigoEnviadoAlGps.set(codigo);
            return new UbicacionBolsin(numero, -31.4201, -64.1888, LocalDateTime.of(2026, 3, 7, 12, 30));
        };
        ConsultarUbicacionBolsines useCase = new ConsultarUbicacionBolsines(repository, gpsTracker);
        Empleado empleado = new Empleado(
                "Ana",
                "Perez",
                "ana@example.com",
                new ComisionMedica(codigoComisionDelEmpleado, "Cordoba")
        );

        List<SeguimientoBolsin> ubicaciones = useCase.ejecutar(empleado);

        assertEquals(codigoComisionDelEmpleado, codigoConsultadoEnRepositorio.get());
        assertEquals(codigoComisionDelEmpleado, codigoEnviadoAlGps.get());
        assertEquals(101, ubicaciones.get(0).numeroBolsin());
        assertEquals("PREC-101", ubicaciones.get(0).numeroPrecinto());
        assertEquals("CM-ROS-02", ubicaciones.get(0).codigoComisionMedicaDestino());
    }

    @Test
    void cancelarDuranteUnaUbicacionNoConsultaLosBolsinesPendientesNiDevuelveResultados() {
        CancellationToken cancellationToken = new CancellationToken();
        AtomicInteger gpsRequests = new AtomicInteger();
        BolsinRepository repository = codigo -> List.of(
                bolsín(101),
                bolsín(102)
        );
        GpsTrackerPort gpsTracker = (numero, codigo) -> {
            gpsRequests.incrementAndGet();
            cancellationToken.cancel();
            return new UbicacionBolsin(numero, -31.4201, -64.1888, LocalDateTime.of(2026, 3, 7, 12, 30));
        };
        ConsultarUbicacionBolsines useCase = new ConsultarUbicacionBolsines(repository, gpsTracker);

        assertThrows(CancellationException.class, () -> useCase.ejecutar(empleado(), cancellationToken));

        assertEquals(1, gpsRequests.get());
    }

    private Bolsin bolsín(Integer numero) {
        return new Bolsin(numero, LocalDate.of(2026, 3, 7), "PREC-" + numero, 2.5,
                new ComisionMedica("CM-CBA-01", "Cordoba"),
                new ComisionMedica("CM-ROS-02", "Rosario"), EstadoBolsin.ENVIADO);
    }

    private Empleado empleado() {
        return new Empleado("Ana", "Perez", "ana@example.com", new ComisionMedica("CM-CBA-01", "Cordoba"));
    }
}
