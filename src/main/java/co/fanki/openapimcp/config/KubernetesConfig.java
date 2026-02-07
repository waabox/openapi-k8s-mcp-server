package co.fanki.openapimcp.config;

import co.fanki.openapimcp.domain.service.OpenApiUrlResolver;
import co.fanki.openapimcp.domain.service.ServiceBackoffTracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Kubernetes integration.
 *
 * <p>The actual configuration values are read from application.yml
 * and injected into the Kubernetes components.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Configuration
public class KubernetesConfig {

    /**
     * Creates the OpenApiUrlResolver bean.
     *
     * <p>Uses the configured URL template if provided, otherwise the resolver
     * will fall back to the default cluster-ip:port based URLs.</p>
     *
     * @param urlTemplate the URL template from configuration, may be empty
     * @return the configured OpenApiUrlResolver
     */
    @Bean
    public OpenApiUrlResolver openApiUrlResolver(
            @Value("${kubernetes.openapi-url-template:}") final String urlTemplate) {
        return new OpenApiUrlResolver(urlTemplate);
    }

    /**
     * Creates the ServiceBackoffTracker bean.
     *
     * <p>Tracks service failures and implements exponential backoff to avoid
     * overwhelming failing services in large clusters.</p>
     *
     * @param maxFailures consecutive failures before applying backoff
     * @param baseBackoffSeconds base backoff duration
     * @param maxBackoffSeconds maximum backoff duration
     * @return the configured ServiceBackoffTracker
     */
    @Bean
    public ServiceBackoffTracker serviceBackoffTracker(
            @Value("${openapi.backoff.max-failures:3}") final int maxFailures,
            @Value("${openapi.backoff.base-seconds:60}") final int baseBackoffSeconds,
            @Value("${openapi.backoff.max-seconds:3600}") final int maxBackoffSeconds) {
        return new ServiceBackoffTracker(maxFailures, baseBackoffSeconds, maxBackoffSeconds);
    }
}
