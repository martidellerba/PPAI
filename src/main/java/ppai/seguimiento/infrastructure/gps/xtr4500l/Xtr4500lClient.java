package ppai.seguimiento.infrastructure.gps.xtr4500l;

import ppai.seguimiento.infrastructure.gps.xtr4500l.dto.XtrBolsinLocationResponseDto;
import ppai.seguimiento.application.port.CancellationToken;
import ppai.seguimiento.application.usecase.GpsTrackerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class Xtr4500lClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient cancellableHttpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Autowired
    public Xtr4500lClient(
            RestClient.Builder restClientBuilder,
            @Value("${gps.xtr4500l.base-url}") String baseUrl,
            @Value("${gps.xtr4500l.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    public Xtr4500lClient(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String apiKey
    ) {
        this(restClientBuilder, baseUrl, apiKey, new ObjectMapper().findAndRegisterModules());
    }

    public XtrBolsinLocationResponseDto getBolsinLocation(
            Integer numeroBolsin,
            String codigoComisionMedicaOrigen
    ) {
        if (apiKey.isBlank()) {
            throw new GpsTrackerException("Falta configurar la API key GPS_XTR_API_KEY.");
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/xtr-4500l/getBolsinLocation")
                            .queryParam("numeroBolsin", numeroBolsin)
                            .queryParam("codigoComisionMedicaOrigen", codigoComisionMedicaOrigen)
                            .build())
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(XtrBolsinLocationResponseDto.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new GpsTrackerException(
                        "El GPS XTR-4500L rechazó la API key. Configure la misma GPS_XTR_API_KEY en el mock y en PPAI.",
                        exception
                );
            }
            if (exception.getStatusCode().value() == 404) {
                throw new GpsTrackerException(
                        "El GPS XTR-4500L no encontró el bolsín o la comisión de origen enviados.",
                        exception
                );
            }
            throw new GpsTrackerException(
                    "La API XTR-4500L respondió con HTTP " + exception.getStatusCode().value() + ".",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new GpsTrackerException(
                    "No se pudo conectar con la API XTR-4500L en " + baseUrl + ". Verifique que el mock esté iniciado.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new GpsTrackerException("La respuesta de la API XTR-4500L no tiene un formato válido.", exception);
        }
    }

    /** Performs the XTR request asynchronously so the consultation token can abort its active HTTP exchange. */
    public XtrBolsinLocationResponseDto getBolsinLocation(
            Integer numeroBolsin,
            String codigoComisionMedicaOrigen,
            CancellationToken cancellationToken
    ) {
        if (apiKey.isBlank()) {
            throw new GpsTrackerException("Falta configurar la API key GPS_XTR_API_KEY.");
        }
        cancellationToken.throwIfCancelled();

        String encodedCommission = URLEncoder.encode(codigoComisionMedicaOrigen, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                + "/api/xtr-4500l/getBolsinLocation?numeroBolsin=" + numeroBolsin
                + "&codigoComisionMedicaOrigen=" + encodedCommission);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("X-API-Key", apiKey)
                .GET()
                .build();
        CompletableFuture<HttpResponse<String>> activeRequest = cancellableHttpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        Runnable abortRequest = () -> activeRequest.cancel(true);
        cancellationToken.registerCancellationHandler(abortRequest);
        try {
            cancellationToken.throwIfCancelled();
            HttpResponse<String> response = activeRequest.get();
            cancellationToken.throwIfCancelled();
            if (response.statusCode() == 401) {
                throw new GpsTrackerException("El GPS XTR-4500L rechazó la API key.");
            }
            if (response.statusCode() == 404) {
                throw new GpsTrackerException("El GPS XTR-4500L no encontró el bolsín o la comisión de origen enviados.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GpsTrackerException("La API XTR-4500L respondió con HTTP " + response.statusCode() + ".");
            }
            return objectMapper.readValue(response.body(), XtrBolsinLocationResponseDto.class);
        } catch (CancellationException exception) {
            cancellationToken.throwIfCancelled();
            throw new GpsTrackerException("La solicitud al GPS XTR-4500L fue interrumpida.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancellationToken.throwIfCancelled();
            throw new GpsTrackerException("La solicitud al GPS XTR-4500L fue interrumpida.", exception);
        } catch (ExecutionException exception) {
            cancellationToken.throwIfCancelled();
            throw new GpsTrackerException("No se pudo conectar con la API XTR-4500L en " + baseUrl + ".", exception.getCause());
        } catch (JsonProcessingException exception) {
            throw new GpsTrackerException("La respuesta de la API XTR-4500L no tiene un formato válido.", exception);
        } finally {
            cancellationToken.clearCancellationHandler(abortRequest);
            if (cancellationToken.isCancelled() && !activeRequest.isDone()) {
                activeRequest.cancel(true);
            }
        }
    }
}
