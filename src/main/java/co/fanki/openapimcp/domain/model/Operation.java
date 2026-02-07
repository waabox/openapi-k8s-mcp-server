package co.fanki.openapimcp.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Value Object representing a single HTTP operation from an OpenAPI specification.
 *
 * <p>An operation corresponds to a single endpoint with a specific HTTP method.</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class Operation {

    private final String operationId;
    private final String method;
    private final String path;
    private final String summary;
    private final String description;
    private final String tag;
    private final List<OperationParameter> parameters;
    private final String requestBodySchema;
    private final String responseSchema;

    private Operation(
            final String operationId,
            final String method,
            final String path,
            final String summary,
            final String description,
            final String tag,
            final List<OperationParameter> parameters,
            final String requestBodySchema,
            final String responseSchema) {
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");
        this.method = Objects.requireNonNull(method, "method cannot be null").toUpperCase();
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.summary = summary;
        this.description = description;
        this.tag = tag;
        this.parameters = parameters == null ? List.of() : List.copyOf(parameters);
        this.requestBodySchema = requestBodySchema;
        this.responseSchema = responseSchema;
    }

    /**
     * Creates a new Operation using a builder pattern.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the unique operation identifier.
     *
     * @return the operationId
     */
    public String operationId() {
        return operationId;
    }

    /**
     * Returns the HTTP method (GET, POST, PUT, DELETE, etc.).
     *
     * @return the method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the endpoint path with path parameters as templates.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Returns a brief summary of the operation.
     *
     * @return the summary, may be null
     */
    public String summary() {
        return summary;
    }

    /**
     * Returns the detailed description of the operation.
     *
     * @return the description, may be null
     */
    public String description() {
        return description;
    }

    /**
     * Returns the OpenAPI tag grouping this operation.
     *
     * @return the tag, may be null
     */
    public String tag() {
        return tag;
    }

    /**
     * Returns the list of parameters for this operation.
     *
     * @return immutable list of parameters
     */
    public List<OperationParameter> parameters() {
        return parameters;
    }

    /**
     * Returns the JSON schema for the request body.
     *
     * @return the request body schema, may be null
     */
    public String requestBodySchema() {
        return requestBodySchema;
    }

    /**
     * Returns the JSON schema for the response.
     *
     * @return the response schema, may be null
     */
    public String responseSchema() {
        return responseSchema;
    }

    /**
     * Returns the path parameters.
     *
     * @return list of path parameters
     */
    public List<OperationParameter> pathParameters() {
        return parameters.stream()
            .filter(OperationParameter::isPathParam)
            .toList();
    }

    /**
     * Returns the query parameters.
     *
     * @return list of query parameters
     */
    public List<OperationParameter> queryParameters() {
        return parameters.stream()
            .filter(OperationParameter::isQueryParam)
            .toList();
    }

    /**
     * Checks if this operation has a request body.
     *
     * @return true if has request body
     */
    public boolean hasRequestBody() {
        return requestBodySchema != null && !requestBodySchema.isBlank();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Operation operation = (Operation) o;
        return operationId.equals(operation.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId);
    }

    @Override
    public String toString() {
        return "Operation{"
            + "operationId='" + operationId + '\''
            + ", method='" + method + '\''
            + ", path='" + path + '\''
            + '}';
    }

    /**
     * Builder for creating Operation instances.
     */
    public static final class Builder {
        private String operationId;
        private String method;
        private String path;
        private String summary;
        private String description;
        private String tag;
        private List<OperationParameter> parameters;
        private String requestBodySchema;
        private String responseSchema;

        private Builder() {
        }

        public Builder operationId(final String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder method(final String method) {
            this.method = method;
            return this;
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public Builder summary(final String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder tag(final String tag) {
            this.tag = tag;
            return this;
        }

        public Builder parameters(final List<OperationParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder requestBodySchema(final String requestBodySchema) {
            this.requestBodySchema = requestBodySchema;
            return this;
        }

        public Builder responseSchema(final String responseSchema) {
            this.responseSchema = responseSchema;
            return this;
        }

        public Operation build() {
            return new Operation(
                operationId,
                method,
                path,
                summary,
                description,
                tag,
                parameters,
                requestBodySchema,
                responseSchema
            );
        }
    }
}
