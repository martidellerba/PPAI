package ppai.seguimiento.infrastructure.notification;

import ppai.seguimiento.application.port.NotificacionSeguimientoPort;
import ppai.seguimiento.domain.model.NotificacionSeguimiento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MicrosoftGraphNotificationAdapter implements NotificacionSeguimientoPort {

    private final RestClient restClient;
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String senderEmail;

    public MicrosoftGraphNotificationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${mail.graph.tenant-id:}") String tenantId,
            @Value("${mail.graph.client-id:}") String clientId,
            @Value("${mail.graph.client-secret:}") String clientSecret,
            @Value("${mail.graph.sender-email:}") String senderEmail
    ) {
        this.restClient = restClientBuilder.build();
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.senderEmail = senderEmail;
    }

    @Override
    public void notificar(NotificacionSeguimiento notificacion) {
        validarConfiguracion();
        String accessToken = obtenerAccessToken();
        String cuerpo = String.format(
                Locale.ROOT,
                "Seguimiento de bolsín\n"
                        + "Número de bolsín: %d\n"
                        + "Número de precinto: %s\n"
                        + "Comisión de origen: %s (%s)\n"
                        + "Comisión de destino: %s (%s)\n"
                        + "Latitud: %s\n"
                        + "Longitud: %s\n"
                        + "Fecha y hora de última actualización GPS: %s",
                notificacion.numeroBolsin(),
                notificacion.numeroPrecinto(),
                notificacion.comisionMedicaOrigen(),
                notificacion.codigoComisionMedicaOrigen(),
                notificacion.comisionMedicaDestino(),
                notificacion.codigoComisionMedicaDestino(),
                notificacion.latitud(),
                notificacion.longitud(),
                notificacion.fechaHoraUltimaActualizacion()
        );
        Map<String, Object> message = Map.of(
                "subject", "Ubicación del bolsín " + notificacion.numeroBolsin(),
                "body", Map.of("contentType", "Text", "content", cuerpo),
                "toRecipients", List.of(Map.of(
                        "emailAddress", Map.of("address", notificacion.emailGerenteDestino())
                ))
        );

        try {
            restClient.post()
                    .uri("https://graph.microsoft.com/v1.0/users/{sender}/sendMail", senderEmail)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(Map.of("message", message, "saveToSentItems", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            String detail = exception.getStatusCode().value() == HttpStatus.FORBIDDEN.value()
                    ? "Microsoft Graph denegó el envío. Verifique Mail.Send como permiso de aplicación, el consentimiento administrativo y el acceso del buzón remitente."
                    : "Microsoft Graph rechazó el envío del correo de seguimiento (HTTP "
                            + exception.getStatusCode().value() + ").";
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    detail,
                    exception
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con Microsoft Graph para enviar el correo.",
                    exception
            );
        }
    }

    private String obtenerAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");

        try {
            Map<?, ?> token = restClient.post()
                    .uri("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", tenantId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            Object accessToken = token == null ? null : token.get("access_token");
            if (!(accessToken instanceof String value) || value.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Microsoft Entra no devolvió un token para Microsoft Graph."
                );
            }
            return value;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo autenticar la aplicación contra Microsoft Graph. Revise la configuración de correo.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con Microsoft Entra para obtener el token de correo.",
                    exception
            );
        }
    }

    private void validarConfiguracion() {
        if (tenantId.isBlank() || clientId.isBlank() || clientSecret.isBlank() || senderEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar MAIL_GRAPH_TENANT_ID, MAIL_GRAPH_CLIENT_ID, MAIL_GRAPH_CLIENT_SECRET y MAIL_GRAPH_SENDER_EMAIL."
            );
        }
    }
}
