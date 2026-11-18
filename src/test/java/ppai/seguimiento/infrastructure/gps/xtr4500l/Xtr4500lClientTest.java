package ppai.seguimiento.infrastructure.gps.xtr4500l;

import com.sun.net.httpserver.HttpServer;
import ppai.seguimiento.application.port.CancellationToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class Xtr4500lClientTest {

    @Test
    void cancelaLaSolicitudHttpXtrQueEstaEnCurso() throws Exception {
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseServer = new CountDownLatch(1);
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        ExecutorService callerExecutor = Executors.newSingleThreadExecutor();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverExecutor);
        server.createContext("/api/xtr-4500l/getBolsinLocation", exchange -> {
            requestStarted.countDown();
            try {
                releaseServer.await(5, TimeUnit.SECONDS);
                byte[] body = "{\"items\":[]}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            Xtr4500lClient client = new Xtr4500lClient(
                    RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key"
            );
            CancellationToken token = new CancellationToken();
            Future<?> pendingCall = callerExecutor.submit(() -> client.getBolsinLocation(101, "CM-CBA-01", token));

            assertTrue(requestStarted.await(2, TimeUnit.SECONDS), "El mock debe recibir la solicitud XTR");
            assertTrue(token.cancel());
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> pendingCall.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof CancellationException);
        } finally {
            releaseServer.countDown();
            server.stop(0);
            callerExecutor.shutdownNow();
            serverExecutor.shutdownNow();
            callerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            serverExecutor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void consultaLaApiXtrConParametrosYApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Xtr4500lClient client = new Xtr4500lClient(builder, "http://gps.test", "test-key");
        server.expect(requestTo("http://gps.test/api/xtr-4500l/getBolsinLocation?numeroBolsin=101&codigoComisionMedicaOrigen=CM-CBA-01"))
                .andExpect(method(GET))
                .andExpect(queryParam("numeroBolsin", "101"))
                .andExpect(queryParam("codigoComisionMedicaOrigen", "CM-CBA-01"))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess(
                        "{\"items\":[{\"numeroBolsin\":101,\"latitud\":-31.4201,\"longitud\":-64.1888,\"fechaHoraUltimaActualizacion\":\"2026-03-07T12:30:00\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        var response = client.getBolsinLocation(101, "CM-CBA-01");

        assertEquals(101, response.items().get(0).numeroBolsin());
        server.verify();
    }

    @Test
    void convierteErroresDelProveedorEnBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Xtr4500lClient client = new Xtr4500lClient(builder, "http://gps.test", "test-key");
        server.expect(requestTo("http://gps.test/api/xtr-4500l/getBolsinLocation?numeroBolsin=101&codigoComisionMedicaOrigen=CM-CBA-01"))
                .andRespond(withUnauthorizedRequest());

        GpsTrackerException exception = assertThrows(
                GpsTrackerException.class,
                () -> client.getBolsinLocation(101, "CM-CBA-01")
        );

        assertEquals(
                "El GPS XTR-4500L rechaz\u00f3 la API key. Configure la misma GPS_XTR_API_KEY en el mock y en PPAI.",
                exception.getMessage()
        );
        server.verify();
    }

    @Test
    void informaLaFaltaDeApiKeySinEnviarUnaSolicitud() {
        Xtr4500lClient client = new Xtr4500lClient(RestClient.builder(), "http://gps.test", " ");

        GpsTrackerException exception = assertThrows(
                GpsTrackerException.class,
                () -> client.getBolsinLocation(101, "CM-CBA-01")
        );

        assertEquals("Falta configurar la API key GPS_XTR_API_KEY.", exception.getMessage());
    }
}
