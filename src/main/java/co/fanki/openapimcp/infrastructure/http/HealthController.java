package co.fanki.openapimcp.infrastructure.http;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health check controller.
 *
 * <p>Provides a lightweight health endpoint for Kubernetes probes.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@RestController
public class HealthController {

    /**
     * Health check endpoint.
     *
     * @return "ok" if the server is healthy
     */
    @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "ok";
    }
}
