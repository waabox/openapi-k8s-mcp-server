package co.fanki.openapimcp.config;

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

    // Configuration is handled via @Value annotations in the Kubernetes components
    // This class serves as a marker and can be extended for future configuration needs

}
