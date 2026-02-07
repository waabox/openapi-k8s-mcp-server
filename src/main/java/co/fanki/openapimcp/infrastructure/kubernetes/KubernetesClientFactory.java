package co.fanki.openapimcp.infrastructure.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;

/**
 * Factory for creating Kubernetes API clients.
 *
 * <p>Supports both in-cluster and out-of-cluster (kubeconfig) configurations.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class KubernetesClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClientFactory.class);

    @Value("${kubernetes.in-cluster:false}")
    private boolean inCluster;

    @Value("${kubernetes.kubeconfig-path:~/.kube/config}")
    private String kubeconfigPath;

    private ApiClient apiClient;

    /**
     * Initializes the Kubernetes client after construction.
     */
    @PostConstruct
    public void init() {
        try {
            if (inCluster) {
                LOG.info("Configuring Kubernetes client for in-cluster mode");
                apiClient = ClientBuilder.cluster().build();
            } else {
                LOG.info("Configuring Kubernetes client from kubeconfig: {}", kubeconfigPath);
                final String resolvedPath = resolvePath(kubeconfigPath);
                final KubeConfig kubeConfig = KubeConfig.loadKubeConfig(
                    new FileReader(resolvedPath)
                );
                apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
            }

            Configuration.setDefaultApiClient(apiClient);
            LOG.info("Kubernetes client initialized successfully");

        } catch (final IOException e) {
            LOG.error("Failed to initialize Kubernetes client: {}", e.getMessage());
            throw new KubernetesClientException(
                "Failed to initialize Kubernetes client", e
            );
        }
    }

    /**
     * Returns the configured API client.
     *
     * @return the Kubernetes API client
     */
    public ApiClient getApiClient() {
        if (apiClient == null) {
            throw new IllegalStateException("Kubernetes client not initialized");
        }
        return apiClient;
    }

    private String resolvePath(final String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    /**
     * Exception thrown when Kubernetes client initialization fails.
     */
    public static class KubernetesClientException extends RuntimeException {

        /**
         * Creates a new KubernetesClientException.
         *
         * @param message the error message
         * @param cause the underlying cause
         */
        public KubernetesClientException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
