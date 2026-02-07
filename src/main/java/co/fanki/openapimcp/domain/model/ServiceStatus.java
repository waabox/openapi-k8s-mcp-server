package co.fanki.openapimcp.domain.model;

/**
 * Enumeration representing the possible states of a discovered service.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public enum ServiceStatus {

    /**
     * Service is active and has a valid OpenAPI specification.
     */
    ACTIVE("Service is active and reachable with valid OpenAPI spec"),

    /**
     * Service exists but is not reachable (network issue, pod not ready, etc.).
     */
    UNREACHABLE("Service is not reachable"),

    /**
     * Service is reachable but does not expose an OpenAPI specification.
     */
    NO_OPENAPI("Service does not expose an OpenAPI specification");

    private final String description;

    ServiceStatus(final String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable description of this status.
     *
     * @return the status description
     */
    public String description() {
        return description;
    }

    /**
     * Checks if the service is in a healthy state.
     *
     * @return true if the service is active
     */
    public boolean isHealthy() {
        return this == ACTIVE;
    }
}
