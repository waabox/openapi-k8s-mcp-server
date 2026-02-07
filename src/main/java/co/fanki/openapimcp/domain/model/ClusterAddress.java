package co.fanki.openapimcp.domain.model;

import java.util.Objects;

/**
 * Value Object representing a network address within the Kubernetes cluster.
 *
 * <p>Contains the IP address and port for reaching a service within the cluster.</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class ClusterAddress {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final String ip;
    private final int port;

    private ClusterAddress(final String ip, final int port) {
        this.ip = Objects.requireNonNull(ip, "ip cannot be null");
        if (ip.isBlank()) {
            throw new IllegalArgumentException("ip cannot be blank");
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(
                "port must be between " + MIN_PORT + " and " + MAX_PORT + ", got: " + port
            );
        }
        this.port = port;
    }

    /**
     * Creates a new ClusterAddress from IP and port.
     *
     * @param ip the cluster IP address
     * @param port the service port
     * @return a new ClusterAddress instance
     * @throws NullPointerException if ip is null
     * @throws IllegalArgumentException if ip is blank or port is out of range
     */
    public static ClusterAddress of(final String ip, final int port) {
        return new ClusterAddress(ip, port);
    }

    /**
     * Returns the cluster IP address.
     *
     * @return the IP address
     */
    public String ip() {
        return ip;
    }

    /**
     * Returns the service port.
     *
     * @return the port number
     */
    public int port() {
        return port;
    }

    /**
     * Builds a complete URL for the given path.
     *
     * @param path the URL path (should start with /)
     * @return the complete URL
     */
    public String toUrl(final String path) {
        final String normalizedPath = path == null ? "" : path;
        final String pathWithSlash = normalizedPath.startsWith("/")
            ? normalizedPath
            : "/" + normalizedPath;
        return "http://" + ip + ":" + port + pathWithSlash;
    }

    /**
     * Returns the base URL without path.
     *
     * @return the base URL
     */
    public String baseUrl() {
        return "http://" + ip + ":" + port;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ClusterAddress that = (ClusterAddress) o;
        return port == that.port && ip.equals(that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "ClusterAddress{" + ip + ":" + port + "}";
    }
}
