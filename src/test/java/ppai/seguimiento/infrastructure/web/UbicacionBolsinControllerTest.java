package ppai.seguimiento.infrastructure.web;

import ppai.seguimiento.application.port.GpsTrackerPort;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import ppai.seguimiento.domain.model.UbicacionBolsin;
import ppai.seguimiento.infrastructure.persistence.repository.SpringDataBolsinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ppai-web-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UbicacionBolsinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataBolsinRepository springDataBolsinRepository;

    @MockBean
    private GpsTrackerPort gpsTracker;

    @Test
    void requiereEmpleadoLogueadoEnLaSesion() throws Exception {
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seguimiento/sesion"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/seguimiento/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaUbicacionesYRegistraNotificacionInterna() throws Exception {
        when(gpsTracker.obtenerUbicacion(anyInt(), eq("CM-CBA-01"), any(CancellationToken.class)))
                .thenAnswer(invocation -> new UbicacionBolsin(
                        invocation.getArgument(0),
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

        mockMvc.perform(get("/api/seguimiento/sesion").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.comisionMedicaAsignada.codigo").value("CM-CBA-01"));

        UUID consultaId = registrarConsulta(session);
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].numeroBolsin").value(101))
                .andExpect(jsonPath("$[0].numeroPrecinto").value("PREC-101"))
                .andExpect(jsonPath("$[0].comisionMedicaDestino").value("Rosario"))
                .andExpect(jsonPath("$[0].latitud").value(-31.4201));

        mockMvc.perform(post("/api/seguimiento/bolsines/101/notificacion").session(session))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.registradaAhora").value(true))
                .andExpect(jsonPath("$.mensaje").value(
                        "Notificación enviada a la bandeja de Rosario (CM-ROS-02)."
                ));
        mockMvc.perform(post("/api/seguimiento/bolsines/101/notificacion").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registradaAhora").value(false))
                .andExpect(jsonPath("$.mensaje").value(
                        "La notificación ya estaba en la bandeja de Rosario (CM-ROS-02)."
                ));
        UUID consultaActualizada = registrarConsulta(session);
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion")
                        .param("consultaId", consultaActualizada.toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificacionRegistrada").value(true));

        MockHttpSession luis = iniciarSesion("luis@example.com", "demo1234");
        mockMvc.perform(get("/api/seguimiento/notificaciones").session(luis))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].numeroBolsin").value(101));
    }

    @Test
    void registraNotificacionSinCorreoNiConsultaGps() throws Exception {
        MockHttpSession session = crearSesionDePrueba();

        mockMvc.perform(post("/api/seguimiento/bolsines/111/notificacion").session(session))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.registradaAhora").value(true))
                .andExpect(jsonPath("$.mensaje").value(
                        "Notificación enviada a la bandeja de Rosario (CM-ROS-02)."
                ));
        mockMvc.perform(post("/api/seguimiento/bolsines/111/notificacion").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registradaAhora").value(false));

        org.junit.jupiter.api.Assertions.assertTrue(
                springDataBolsinRepository.findById(111).orElseThrow().isNotificacionRegistrada()
        );
        verifyNoInteractions(gpsTracker);
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
        mockMvc.perform(delete("/api/sesion").session(session))
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
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/seguimiento/consultas/{consultaId}/cancelacion", consultaId).session(session))
                .andExpect(status().isConflict())
                .andExpect(content().string("La consulta ya finalizó o ya fue cancelada."));

        verifyNoInteractions(gpsTracker);
    }

    @Test
    void cancelarConsultaEnCursoInterrumpeLaUbicacionGps() throws Exception {
        CountDownLatch gpsStarted = new CountDownLatch(1);
        CountDownLatch gpsCancelled = new CountDownLatch(1);
        when(gpsTracker.obtenerUbicacion(anyInt(), eq("CM-CBA-01"), any(CancellationToken.class)))
                .thenAnswer(invocation -> {
                    CancellationToken token = invocation.getArgument(2);
                    token.registerCancellationHandler(gpsCancelled::countDown);
                    gpsStarted.countDown();
                    if (!gpsCancelled.await(2, TimeUnit.SECONDS)) {
                        throw new AssertionError("El GPS no recibió la señal de cancelación.");
                    }
                    token.throwIfCancelled();
                    return null;
                });
        MockHttpSession session = crearSesionDePrueba();
        UUID consultaId = registrarConsulta(session);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<MvcResult> consultation = executor.submit(() -> mockMvc.perform(
                    get("/api/seguimiento/consultar-ubicacion")
                            .param("consultaId", consultaId.toString())
                            .session(session)
            ).andReturn());
            assertTrue(gpsStarted.await(2, TimeUnit.SECONDS), "La consulta debe llegar al GPS");

            mockMvc.perform(post("/api/seguimiento/consultas/{consultaId}/cancelacion", consultaId).session(session))
                    .andExpect(status().isNoContent());

            assertTrue(gpsCancelled.await(2, TimeUnit.SECONDS), "La cancelación debe llegar al GPS");
            org.junit.jupiter.api.Assertions.assertEquals(
                    409,
                    consultation.get(2, TimeUnit.SECONDS).getResponse().getStatus()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requiereCredencialesValidasParaIniciarSesion() throws Exception {
        mockMvc.perform(post("/api/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Correo o contraseña incorrectos."));
    }

    @Test
    void cadaEmpleadoObtieneUnaSesionConSuPropiaIdentidad() throws Exception {
        when(gpsTracker.obtenerUbicacion(anyInt(), eq("CM-ROS-02"), any(CancellationToken.class)))
                .thenAnswer(invocation -> new UbicacionBolsin(
                        invocation.getArgument(0),
                        -32.9468,
                        -60.6393,
                        LocalDateTime.of(2026, 3, 7, 12, 32)
                ));
        MockHttpSession ana = crearSesionDePrueba();
        MockHttpSession luis = iniciarSesion("luis@example.com", "demo1234");

        mockMvc.perform(get("/api/seguimiento/sesion").session(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
        mockMvc.perform(get("/api/seguimiento/sesion").session(luis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("luis@example.com"))
                .andExpect(jsonPath("$.comisionMedicaAsignada.codigo").value("CM-ROS-02"));

        UUID consultaLuis = registrarConsulta(luis);
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion")
                        .param("consultaId", consultaLuis.toString())
                        .session(luis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroBolsin").value(102));
    }

    @Test
    void informaErrorDeGatewayCuandoElGpsNoDevuelveUbicacion() throws Exception {
        when(gpsTracker.obtenerUbicacion(anyInt(), eq("CM-CBA-01"), any(CancellationToken.class)))
                .thenThrow(new GpsTrackerException("No se pudo consultar la API XTR-4500L."));
        MockHttpSession session = crearSesionDePrueba();
        UUID consultaId = registrarConsulta(session);
        mockMvc.perform(get("/api/seguimiento/consultar-ubicacion")
                        .param("consultaId", consultaId.toString())
                        .session(session))
                .andExpect(status().isBadGateway());
    }

    private MockHttpSession crearSesionDePrueba() throws Exception {
        return iniciarSesion("ana@example.com", "demo1234");
    }

    private MockHttpSession iniciarSesion(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
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
