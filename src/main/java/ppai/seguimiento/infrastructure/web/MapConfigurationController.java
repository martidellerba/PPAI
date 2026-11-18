package ppai.seguimiento.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class MapConfigurationController {

    private final String googleMapsApiKey;

    public MapConfigurationController(@Value("${maps.google.api-key:}") String googleMapsApiKey) {
        this.googleMapsApiKey = googleMapsApiKey;
    }

    @GetMapping("/maps")
    public MapConfigurationDto obtenerConfiguracionMapa() {
        return new MapConfigurationDto(googleMapsApiKey);
    }

    public record MapConfigurationDto(String googleMapsApiKey) {
    }
}
