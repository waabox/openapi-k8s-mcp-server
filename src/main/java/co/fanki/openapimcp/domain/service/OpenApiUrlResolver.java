package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.DiscoveredService;

/**
 * Domain service that resolves OpenAPI specification URLs for discovered services.
 *
 * <p>Supports configurable URL templates with placeholders that are replaced
 * with actual service values at resolution time.</p>
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *   <li>{@code {service-name}} - Kubernetes service name</li>
 *   <li>{@code {namespace}} - Kubernetes namespace</li>
 *   <li>{@code {cluster-ip}} - Service cluster IP</li>
 *   <li>{@code {port}} - Service port</li>
 * </ul>
 *
 * <p>Example template: {@code http://{service-name}.svc.fanki.co/{service-name}/v3/api-docs}</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class OpenApiUrlResolver {

    private static final String PLACEHOLDER_SERVICE_NAME = "{service-name}";
    private static final String PLACEHOLDER_NAMESPACE = "{namespace}";
    private static final String PLACEHOLDER_CLUSTER_IP = "{cluster-ip}";
    private static final String PLACEHOLDER_PORT = "{port}";

    private final String urlTemplate;

    /**
     * Creates a new OpenApiUrlResolver with the specified URL template.
     *
     * @param urlTemplate the URL template with placeholders, may be null or blank
     *                    to use default behavior
     */
    public OpenApiUrlResolver(final String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    /**
     * Resolves the OpenAPI specification URL for the given service.
     *
     * <p>If a URL template is configured, replaces all placeholders with the
     * service's actual values. Otherwise, falls back to the service's default
     * OpenAPI URL (cluster-ip:port based).</p>
     *
     * @param service the discovered service to resolve the URL for
     * @return the resolved OpenAPI specification URL
     * @throws NullPointerException if service is null
     */
    public String resolve(final DiscoveredService service) {
        if (service == null) {
            throw new NullPointerException("service cannot be null");
        }

        if (urlTemplate == null || urlTemplate.isBlank()) {
            return service.openApiUrl();
        }

        return urlTemplate
            .replace(PLACEHOLDER_SERVICE_NAME, service.id().name())
            .replace(PLACEHOLDER_NAMESPACE, service.id().namespace())
            .replace(PLACEHOLDER_CLUSTER_IP, service.address().ip())
            .replace(PLACEHOLDER_PORT, String.valueOf(service.address().port()));
    }

    /**
     * Checks if this resolver uses a custom URL template.
     *
     * @return true if a custom template is configured, false otherwise
     */
    public boolean hasCustomTemplate() {
        return urlTemplate != null && !urlTemplate.isBlank();
    }
}
