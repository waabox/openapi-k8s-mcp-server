package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.ClusterAddress;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.OpenApiPath;
import co.fanki.openapimcp.domain.model.ServiceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OpenApiUrlResolver}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class OpenApiUrlResolverTest {

    @Test
    void whenResolving_givenNullTemplate_shouldReturnDefaultUrl() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(null);
        final DiscoveredService service = createService(
            "default", "my-service", "10.0.0.1", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://10.0.0.1:8080/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenBlankTemplate_shouldReturnDefaultUrl() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver("   ");
        final DiscoveredService service = createService(
            "default", "my-service", "10.0.0.1", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://10.0.0.1:8080/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenEmptyTemplate_shouldReturnDefaultUrl() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver("");
        final DiscoveredService service = createService(
            "default", "my-service", "10.0.0.1", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://10.0.0.1:8080/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenTemplateWithServiceName_shouldReplaceServiceName() {
        final String template = "http://{service-name}.svc.fanki.co/v3/api-docs";
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(template);
        final DiscoveredService service = createService(
            "production", "user-service", "10.0.0.1", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://user-service.svc.fanki.co/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenTemplateWithNamespace_shouldReplaceNamespace() {
        final String template = "http://{service-name}.{namespace}.svc.cluster.local/v3/api-docs";
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(template);
        final DiscoveredService service = createService(
            "production", "user-service", "10.0.0.1", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://user-service.production.svc.cluster.local/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenTemplateWithAllPlaceholders_shouldReplaceAll() {
        final String template = "http://{service-name}.{namespace}.svc:{port}/{cluster-ip}/api-docs";
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(template);
        final DiscoveredService service = createService(
            "staging", "api-gateway", "192.168.1.100", 9090
        );

        final String result = resolver.resolve(service);

        assertEquals("http://api-gateway.staging.svc:9090/192.168.1.100/api-docs", result);
    }

    @Test
    void whenResolving_givenTemplateWithMultipleOccurrences_shouldReplaceAll() {
        final String template = "http://{service-name}.svc.fanki.co/{service-name}/v3/api-docs";
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(template);
        final DiscoveredService service = createService(
            "default", "order-service", "10.0.0.5", 8080
        );

        final String result = resolver.resolve(service);

        assertEquals("http://order-service.svc.fanki.co/order-service/v3/api-docs", result);
    }

    @Test
    void whenResolving_givenNullService_shouldThrowException() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(
            "http://{service-name}.svc.fanki.co/v3/api-docs"
        );

        assertThrows(NullPointerException.class, () -> resolver.resolve(null));
    }

    @Test
    void whenCheckingHasCustomTemplate_givenNullTemplate_shouldReturnFalse() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(null);

        assertFalse(resolver.hasCustomTemplate());
    }

    @Test
    void whenCheckingHasCustomTemplate_givenBlankTemplate_shouldReturnFalse() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver("   ");

        assertFalse(resolver.hasCustomTemplate());
    }

    @Test
    void whenCheckingHasCustomTemplate_givenValidTemplate_shouldReturnTrue() {
        final OpenApiUrlResolver resolver = new OpenApiUrlResolver(
            "http://{service-name}.svc.fanki.co/v3/api-docs"
        );

        assertTrue(resolver.hasCustomTemplate());
    }

    private DiscoveredService createService(
            final String namespace,
            final String name,
            final String ip,
            final int port) {
        return DiscoveredService.discover(
            ServiceId.of(namespace, name),
            ClusterAddress.of(ip, port),
            OpenApiPath.defaultPath()
        );
    }
}
