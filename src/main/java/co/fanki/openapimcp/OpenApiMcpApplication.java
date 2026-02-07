package co.fanki.openapimcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstrap class for the OpenAPI MCP Server application.
 *
 * <p>This application discovers Kubernetes services, fetches their OpenAPI
 * specifications, and exposes them through the Model Context Protocol (MCP)
 * for integration with AI assistants.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@SpringBootApplication
@EnableScheduling
public class OpenApiMcpApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(OpenApiMcpApplication.class, args);
    }
}
