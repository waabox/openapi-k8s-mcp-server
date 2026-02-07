package co.fanki.openapimcp.domain.model;

import java.util.Objects;

/**
 * Value Object representing a unique service identifier within the Kubernetes cluster.
 *
 * <p>A ServiceId is composed of the namespace and name of the service,
 * following the Kubernetes naming convention.</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class ServiceId {

    private final String namespace;
    private final String name;

    private ServiceId(final String namespace, final String name) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("namespace cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }

    /**
     * Creates a new ServiceId from namespace and name.
     *
     * @param namespace the Kubernetes namespace
     * @param name the service name
     * @return a new ServiceId instance
     * @throws NullPointerException if namespace or name is null
     * @throws IllegalArgumentException if namespace or name is blank
     */
    public static ServiceId of(final String namespace, final String name) {
        return new ServiceId(namespace, name);
    }

    /**
     * Parses a ServiceId from its string representation.
     *
     * @param value the string in format "namespace/name"
     * @return a new ServiceId instance
     * @throws IllegalArgumentException if the format is invalid
     */
    public static ServiceId fromString(final String value) {
        Objects.requireNonNull(value, "value cannot be null");
        final String[] parts = value.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid ServiceId format. Expected 'namespace/name', got: " + value
            );
        }
        return new ServiceId(parts[0], parts[1]);
    }

    /**
     * Returns the Kubernetes namespace.
     *
     * @return the namespace
     */
    public String namespace() {
        return namespace;
    }

    /**
     * Returns the service name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the string representation in format "namespace/name".
     *
     * @return the composite identifier string
     */
    public String asString() {
        return namespace + "/" + name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ServiceId serviceId = (ServiceId) o;
        return namespace.equals(serviceId.namespace) && name.equals(serviceId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name);
    }

    @Override
    public String toString() {
        return "ServiceId{" + asString() + "}";
    }
}
