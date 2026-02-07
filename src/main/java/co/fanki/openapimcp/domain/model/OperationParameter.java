package co.fanki.openapimcp.domain.model;

import java.util.Objects;

/**
 * Value Object representing a parameter of an HTTP operation.
 *
 * <p>Parameters can be path parameters, query parameters, headers, or cookies.</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class OperationParameter {

    /**
     * The location where the parameter is expected.
     */
    public enum In {
        PATH,
        QUERY,
        HEADER,
        COOKIE
    }

    private final String name;
    private final In in;
    private final boolean required;
    private final String description;
    private final String schema;

    private OperationParameter(
            final String name,
            final In in,
            final boolean required,
            final String description,
            final String schema) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.required = required;
        this.description = description;
        this.schema = schema;
    }

    /**
     * Creates a new OperationParameter.
     *
     * @param name the parameter name
     * @param in where the parameter is expected
     * @param required whether the parameter is required
     * @param description the parameter description (optional)
     * @param schema the JSON schema of the parameter (optional)
     * @return a new OperationParameter instance
     */
    public static OperationParameter of(
            final String name,
            final In in,
            final boolean required,
            final String description,
            final String schema) {
        return new OperationParameter(name, in, required, description, schema);
    }

    /**
     * Creates a required path parameter.
     *
     * @param name the parameter name
     * @param schema the JSON schema
     * @return a new OperationParameter instance
     */
    public static OperationParameter pathParam(final String name, final String schema) {
        return new OperationParameter(name, In.PATH, true, null, schema);
    }

    /**
     * Creates an optional query parameter.
     *
     * @param name the parameter name
     * @param schema the JSON schema
     * @return a new OperationParameter instance
     */
    public static OperationParameter queryParam(final String name, final String schema) {
        return new OperationParameter(name, In.QUERY, false, null, schema);
    }

    /**
     * Returns the parameter name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns where the parameter is expected.
     *
     * @return the parameter location
     */
    public In in() {
        return in;
    }

    /**
     * Checks if the parameter is required.
     *
     * @return true if required
     */
    public boolean required() {
        return required;
    }

    /**
     * Returns the parameter description.
     *
     * @return the description, may be null
     */
    public String description() {
        return description;
    }

    /**
     * Returns the JSON schema of the parameter.
     *
     * @return the schema, may be null
     */
    public String schema() {
        return schema;
    }

    /**
     * Checks if this is a path parameter.
     *
     * @return true if path parameter
     */
    public boolean isPathParam() {
        return in == In.PATH;
    }

    /**
     * Checks if this is a query parameter.
     *
     * @return true if query parameter
     */
    public boolean isQueryParam() {
        return in == In.QUERY;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OperationParameter that = (OperationParameter) o;
        return required == that.required
            && name.equals(that.name)
            && in == that.in;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, in, required);
    }

    @Override
    public String toString() {
        return "OperationParameter{"
            + "name='" + name + '\''
            + ", in=" + in
            + ", required=" + required
            + '}';
    }
}
