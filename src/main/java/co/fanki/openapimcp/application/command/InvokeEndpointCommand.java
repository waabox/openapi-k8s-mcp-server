package co.fanki.openapimcp.application.command;

import co.fanki.openapimcp.domain.model.ServiceId;

import java.util.Map;
import java.util.Objects;

/**
 * Command to invoke an endpoint on a microservice.
 *
 * <p>This command encapsulates all the information needed to make
 * an HTTP call to a discovered microservice endpoint.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class InvokeEndpointCommand {

    private final ServiceId serviceId;
    private final String operationId;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final String body;

    private InvokeEndpointCommand(
            final ServiceId serviceId,
            final String operationId,
            final Map<String, String> pathParams,
            final Map<String, String> queryParams,
            final String body) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId cannot be null");
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");
        this.pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
        this.queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        this.body = body;
    }

    /**
     * Creates a new InvokeEndpointCommand using a builder.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the target service identifier.
     *
     * @return the service ID
     */
    public ServiceId serviceId() {
        return serviceId;
    }

    /**
     * Returns the operation identifier.
     *
     * @return the operation ID
     */
    public String operationId() {
        return operationId;
    }

    /**
     * Returns the path parameters.
     *
     * @return immutable map of path parameters
     */
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /**
     * Returns the query parameters.
     *
     * @return immutable map of query parameters
     */
    public Map<String, String> queryParams() {
        return queryParams;
    }

    /**
     * Returns the request body.
     *
     * @return the body, may be null
     */
    public String body() {
        return body;
    }

    /**
     * Checks if this command has a request body.
     *
     * @return true if has body
     */
    public boolean hasBody() {
        return body != null && !body.isBlank();
    }

    @Override
    public String toString() {
        return "InvokeEndpointCommand{"
            + "serviceId=" + serviceId
            + ", operationId='" + operationId + '\''
            + '}';
    }

    /**
     * Builder for InvokeEndpointCommand.
     */
    public static final class Builder {
        private ServiceId serviceId;
        private String operationId;
        private Map<String, String> pathParams;
        private Map<String, String> queryParams;
        private String body;

        private Builder() {
        }

        public Builder serviceId(final ServiceId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder serviceId(final String serviceIdString) {
            this.serviceId = ServiceId.fromString(serviceIdString);
            return this;
        }

        public Builder operationId(final String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder pathParams(final Map<String, String> pathParams) {
            this.pathParams = pathParams;
            return this;
        }

        public Builder queryParams(final Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder body(final String body) {
            this.body = body;
            return this;
        }

        public InvokeEndpointCommand build() {
            return new InvokeEndpointCommand(
                serviceId, operationId, pathParams, queryParams, body
            );
        }
    }
}
