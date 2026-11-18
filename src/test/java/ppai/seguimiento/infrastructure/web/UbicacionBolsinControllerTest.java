package ppai.seguimiento.infrastructure.web;

import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.port.ComisionMedicaRepository;
import ppai.seguimiento.application.port.NotificacionSeguimientoPort;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import ppai.seguimiento.domain.model.NotificacionSeguimiento;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UbicacionBolsinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GpsTrackerPort gpsTracker;

    @MockBean
    private ComisionMedicaRepository comisionMedicaRepository;

    @MockBean
    private NotificacionSeguimientoPort notificacionPort;

    @Test
    void requiereEmpleadoLogueadoEnLaSesion() throws Exception {
        mockMvc.perform(get("/api/seguimiento/bolsines"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seguimiento/sesion"))
                .andExpect(status().isNoContent());
    }

    @Test
    void consultaUbicacionesYEnvíaNotificacionAlGerenteConfigurado() throws Exception {
        when(gpsTracker.obtenerUbicacion(eq(101), eq("CM-CBA-01"), any(CancellationToken.class)))
                .thenReturn(new UbicacionBolsin(
                        101,
                        -31.4201,
                        -64.1888,
                        LocalDateTime.of(2026, 3, 7, 12, 30)
                ));
        when(gpsTracker.obtenerUbicacion(101, "CM-CBA-01"))
                .thenReturn(new UbicacionBolsin(
                        101,
                        -31.4201,
                        -64.1888,
                        LocalDateTime.of(2026, 3, 7, 12, 30)
                ));
        MockHttpSession session = crearSesionDePrueba();
        when(comisionMedicaRepository.findManagerEmailByCommissionCode("CM-ROS-02"))
                .thenReturn(Optional.of("gerente-real@institution.example"));

        mockMvc.perform(get("/api/seguimiento/sesion").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.comisionMedicaAsignada.codigo").value("CM-CBA-01"));

        UUID consultaId = registrarConsulta(session);
        mockMvc.perform(get("/api/seguimiento/bolsines")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroBolsin").value(101))
                .andExpect(jsonPath("$[0].numeroPrecinto").value("PREC-101"))
                .andExpect(jsonPath("$[0].comisionMedicaDestino").value("Rosario"))
                .andExpect(jsonPath("$[0].latitud").value(-31.4201));

        mockMvc.perform(post("/api/seguimiento/bolsines/101/notificacion").session(session))
                .andExpect(status().isAccepted());
        org.mockito.ArgumentCaptor<NotificacionSeguimiento> notification =
                org.mockito.ArgumentCaptor.forClass(NotificacionSeguimiento.class);
        org.mockito.Mockito.verify(notificacionPort).notificar(notification.capture());
        org.junit.jupiter.api.Assertions.assertEquals(101, notification.getValue().numeroBolsin());
        org.junit.jupiter.api.Assertions.assertEquals("PREC-101", notification.getValue().numeroPrecinto());
        org.junit.jupiter.api.Assertions.assertEquals("CM-CBA-01", notification.getValue().codigoComisionMedicaOrigen());
        org.junit.jupiter.api.Assertions.assertEquals("Cordoba", notification.getValue().comisionMedicaOrigen());
        org.junit.jupiter.api.Assertions.assertEquals("gerente-real@institution.example", notification.getValue().emailGerenteDestino());
        org.junit.jupiter.api.Assertions.assertEquals(-31.4201, notification.getValue().latitud());
        org.junit.jupiter.api.Assertions.assertEquals(-64.1888, notification.getValue().longitud());
    }

    @Test
    void noPermiteNotificarUnBolsinFueraDelAlcanceDelEmpleado() throws Exception {
        MockHttpSession session = crearSesionDePrueba();

        mockMvc.perform(post("/api/seguimiento/bolsines/999/notificacion").session(session))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("999")));
    }

    @Test
    void alCerrarSesionInvalidaLaSesion() throws Exception {
        MockHttpSession session = crearSesionDePrueba();
        mockMvc.perform(delete("/api/dev/sesion").session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/seguimiento/sesion"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelarConsultaRegistradaImpideSuEjecucionGps() throws Exception {
        MockHttpSession session = crearSesionDePrueba();
        UUID consultaId = registrarConsulta(session);

        mockMvc.perform(post("/api/seguimiento/consultas/{consultaId}/cancelacion", consultaId).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/seguimiento/bolsines")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isConflict());

        org.mockito.Mockito.verifyNoInteractions(gpsTracker, notificacionPort);
    }

    @Test
    void informaErrorDeGatewayCuandoElGpsNoDevuelveUbicacion() throws Exception {
        when(gpsTracker.obtenerUbicacion(eq(101), eq("CM-CBA-01"), any(CancellationToken.class)))
                .thenThrow(new GpsTrackerException("No se pudo consultar la API XTR-4500L."));
        MockHttpSession session = crearSesionDePrueba();
        UUID consultaId = registrarConsulta(session);
        mockMvc.perform(get("/api/seguimiento/bolsines")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isBadGateway());
    }

    private MockHttpSession crearSesionDePrueba() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dev/sesion"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UUID registrarConsulta(MockHttpSession session) throws Exception {
        UUID consultaId = UUID.randomUUID();
        mockMvc.perform(post("/api/seguimiento/consultas/{consultaId}", consultaId).session(session))
                .andExpect(status().isNoContent());
        return consultaId;
    }
}
