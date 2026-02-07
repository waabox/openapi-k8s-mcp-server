package co.fanki.openapimcp.domain.model;

import java.util.Objects;

/**
 * Value Object representing the path where a service exposes its OpenAPI specification.
 *
 * <p>The default path follows the Spring Boot convention: /v3/api-docs</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class OpenApiPath {

    private static final String DEFAULT_PATH = "/v3/api-docs";
    private static final OpenApiPath DEFAULT_INSTANCE = new OpenApiPath(DEFAULT_PATH);

    private final String path;

    private OpenApiPath(final String path) {
        Objects.requireNonNull(path, "path cannot be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        this.path = path.startsWith("/") ? path : "/" + path;
    }

    /**
     * Creates an OpenApiPath with a custom path.
     *
     * @param path the custom path
     * @return a new OpenApiPath instance
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if path is blank
     */
    public static OpenApiPath of(final String path) {
        if (DEFAULT_PATH.equals(path) || path.equals("v3/api-docs")) {
            return DEFAULT_INSTANCE;
        }
        return new OpenApiPath(path);
    }

    /**
     * Returns the default OpenAPI path (/v3/api-docs).
     *
     * @return the default OpenApiPath instance
     */
    public static OpenApiPath defaultPath() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns the path value.
     *
     * @return the path
     */
    public String value() {
        return path;
    }

    /**
     * Checks if this is the default path.
     *
     * @return true if this is the default path
     */
    public boolean isDefault() {
        return DEFAULT_PATH.equals(path);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OpenApiPath that = (OpenApiPath) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "OpenApiPath{" + path + "}";
    }
}
