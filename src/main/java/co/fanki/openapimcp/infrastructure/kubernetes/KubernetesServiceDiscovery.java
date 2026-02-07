package co.fanki.openapimcp.infrastructure.kubernetes;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Discovers services in a Kubernetes cluster.
 *
 * <p>Uses the Kubernetes API to list services and extract their
 * connection information for OpenAPI discovery.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class KubernetesServiceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);

    private static final String OPENAPI_PATH_ANNOTATION = "openapi.fanki.co/path";
    private static final String OPENAPI_ENABLED_ANNOTATION = "openapi.fanki.co/enabled";

    private final KubernetesClientFactory clientFactory;

    @Value("${kubernetes.namespace-filter:}")
    private String namespaceFilter;

    @Value("${kubernetes.openapi-path:/v3/api-docs}")
    private String defaultOpenApiPath;

    @Value("${kubernetes.discovery-label:}")
    private String discoveryLabel;

    /**
     * Creates a new KubernetesServiceDiscovery.
     *
     * @param clientFactory the Kubernetes client factory
     */
    public KubernetesServiceDiscovery(final KubernetesClientFactory clientFactory) {
        this.clientFactory = Objects.requireNonNull(clientFactory);
    }

    /**
     * Discovers all services in the cluster that may have OpenAPI specs.
     *
     * @return list of discovered Kubernetes services
     */
    public List<DiscoveredK8sService> discoverServices() {
        LOG.info("Starting Kubernetes service discovery");

        final CoreV1Api api = new CoreV1Api(clientFactory.getApiClient());
        final List<DiscoveredK8sService> discovered = new ArrayList<>();

        try {
            final V1ServiceList serviceList;

            if (namespaceFilter != null && !namespaceFilter.isBlank()) {
                LOG.debug("Discovering services in namespace: {}", namespaceFilter);
                serviceList = api.listNamespacedService(namespaceFilter)
                    .labelSelector(buildLabelSelector())
                    .execute();
            } else {
                LOG.debug("Discovering services in all namespaces");
                serviceList = api.listServiceForAllNamespaces()
                    .labelSelector(buildLabelSelector())
                    .execute();
            }

            if (serviceList.getItems() != null) {
                for (final V1Service service : serviceList.getItems()) {
                    processService(service).ifPresent(discovered::add);
                }
            }

            LOG.info("Discovered {} services", discovered.size());
            return discovered;

        } catch (final ApiException e) {
            LOG.error("Failed to list Kubernetes services: {} - {}",
                e.getCode(), e.getResponseBody());
            throw new ServiceDiscoveryException(
                "Failed to discover Kubernetes services", e
            );
        }
    }

    private String buildLabelSelector() {
        if (discoveryLabel == null || discoveryLabel.isBlank()) {
            return null;
        }
        return discoveryLabel;
    }

    private Optional<DiscoveredK8sService> processService(final V1Service service) {
        if (service.getMetadata() == null || service.getSpec() == null) {
            return Optional.empty();
        }

        final String namespace = service.getMetadata().getNamespace();
        final String name = service.getMetadata().getName();
        final var annotations = service.getMetadata().getAnnotations();

        // Check if OpenAPI is explicitly disabled
        if (annotations != null) {
            final String enabled = annotations.get(OPENAPI_ENABLED_ANNOTATION);
            if ("false".equalsIgnoreCase(enabled)) {
                LOG.debug("Skipping service {}/{}: OpenAPI disabled via annotation",
                    namespace, name);
                return Optional.empty();
            }
        }

        // Get cluster IP
        final String clusterIp = service.getSpec().getClusterIP();
        if (clusterIp == null || "None".equals(clusterIp)) {
            LOG.debug("Skipping headless service {}/{}", namespace, name);
            return Optional.empty();
        }

        // Get port (prefer HTTP ports)
        final Integer port = selectPort(service.getSpec().getPorts());
        if (port == null) {
            LOG.debug("No suitable port found for service {}/{}", namespace, name);
            return Optional.empty();
        }

        // Get OpenAPI path from annotation or use default
        String openApiPath = defaultOpenApiPath;
        if (annotations != null && annotations.containsKey(OPENAPI_PATH_ANNOTATION)) {
            openApiPath = annotations.get(OPENAPI_PATH_ANNOTATION);
        }

        LOG.debug("Discovered service: {}/{} at {}:{}, openapi={}",
            namespace, name, clusterIp, port, openApiPath);

        return Optional.of(new DiscoveredK8sService(
            namespace,
            name,
            clusterIp,
            port,
            openApiPath
        ));
    }

    private Integer selectPort(final List<V1ServicePort> ports) {
        if (ports == null || ports.isEmpty()) {
            return null;
        }

        // Prefer ports named "http" or common HTTP ports
        for (final V1ServicePort port : ports) {
            if (port.getName() != null && port.getName().toLowerCase().contains("http")) {
                return port.getPort();
            }
        }

        // Try common ports
        for (final V1ServicePort port : ports) {
            final int p = port.getPort();
            if (p == 80 || p == 8080 || p == 8000 || p == 3000) {
                return p;
            }
        }

        // Fall back to first port
        return ports.get(0).getPort();
    }

    /**
     * Represents a discovered Kubernetes service.
     *
     * @param namespace the Kubernetes namespace
     * @param name the service name
     * @param clusterIp the cluster IP
     * @param port the service port
     * @param openApiPath the path to the OpenAPI spec
     */
    public record DiscoveredK8sService(
            String namespace,
            String name,
            String clusterIp,
            int port,
            String openApiPath) {

        public DiscoveredK8sService {
            Objects.requireNonNull(namespace);
            Objects.requireNonNull(name);
            Objects.requireNonNull(clusterIp);
            Objects.requireNonNull(openApiPath);
        }
    }

    /**
     * Exception thrown when service discovery fails.
     */
    public static class ServiceDiscoveryException extends RuntimeException {

        /**
         * Creates a new ServiceDiscoveryException.
         *
         * @param message the error message
         * @param cause the underlying cause
         */
        public ServiceDiscoveryException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
