package ppai.seguimiento.infrastructure.notification;

import ppai.seguimiento.domain.model.NotificacionSeguimiento;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class MicrosoftGraphNotificationAdapterTest {

    @Test
    void obtieneTokenYEnvíaAlGerenteTodosLosDatosDelSeguimiento() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MicrosoftGraphNotificationAdapter adapter = new MicrosoftGraphNotificationAdapter(
                builder, "tenant-test", "client-test", "secret-test", "sender@example.test"
        );
        server.expect(requestTo("https://login.microsoftonline.com/tenant-test/oauth2/v2.0/token"))
                .andExpect(method(POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=https%3A%2F%2Fgraph.microsoft.com%2F.default")))
                .andRespond(withSuccess("{\"access_token\":\"token-test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/sender%40example.test/sendMail"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer token-test"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"message":{"subject":"Ubicación del bolsín 101","body":{"contentType":"Text","content":"Seguimiento de bolsín\\nNúmero de bolsín: 101\\nNúmero de precinto: PREC-101\\nComisión de origen: Córdoba (CM-CBA-01)\\nComisión de destino: Rosario (CM-ROS-02)\\nLatitud: -31.42\\nLongitud: -64.18\\nFecha y hora de última actualización GPS: 2026-03-07T12:30"},"toRecipients":[{"emailAddress":{"address":"gerente@example.test"}}]},"saveToSentItems":true}
                        """))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        adapter.notificar(new NotificacionSeguimiento(
                101, "PREC-101", "CM-CBA-01", "Córdoba", "CM-ROS-02", "Rosario",
                "gerente@example.test", LocalDateTime.of(2026, 3, 7, 12, 30), -31.42, -64.18
        ));

        server.verify();
    }

    @Test
    void rechazaEnviarSiFaltaConfiguracionDeCorreo() {
        MicrosoftGraphNotificationAdapter adapter = new MicrosoftGraphNotificationAdapter(
                RestClient.builder(), "", "", "", ""
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adapter.notificar(
                new NotificacionSeguimiento(101, "PREC-101", "CM-CBA-01", "Córdoba", "CM-ROS-02", "Rosario",
                        "manager@example.test", LocalDateTime.now(), -31.42, -64.18)
        ));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void informaQueGraphRechazoPorPermisosCuandoMailSendNoEstaConcedido() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MicrosoftGraphNotificationAdapter adapter = new MicrosoftGraphNotificationAdapter(
                builder, "tenant-test", "client-test", "secret-test", "sender@example.test"
        );
        server.expect(requestTo("https://login.microsoftonline.com/tenant-test/oauth2/v2.0/token"))
                .andRespond(withSuccess("{\"access_token\":\"token-test\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/sender%40example.test/sendMail"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adapter.notificar(
                new NotificacionSeguimiento(101, "PREC-101", "CM-CBA-01", "Córdoba", "CM-ROS-02", "Rosario",
                        "manager@example.test", LocalDateTime.of(2026, 3, 7, 12, 30), -31.42, -64.18)
        ));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Mail.Send"));
        server.verify();
    }
}
