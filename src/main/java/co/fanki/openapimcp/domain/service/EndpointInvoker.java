package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.InvocationResult;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.OperationParameter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Domain service responsible for building endpoint invocation requests.
 *
 * <p>This service handles the domain logic for preparing endpoint invocations,
 * including URL building, parameter substitution, and validation.
 * The actual HTTP call is delegated to the infrastructure layer.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class EndpointInvoker {

    /**
     * Represents a prepared invocation request.
     *
     * @param url the full URL to invoke
     * @param method the HTTP method
     * @param body the request body (may be null)
     * @param headers additional headers
     */
    public record InvocationRequest(
            String url,
            String method,
            String body,
            Map<String, String> headers) {

        public InvocationRequest {
            Objects.requireNonNull(url);
            Objects.requireNonNull(method);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    /**
     * Prepares an invocation request for the given operation.
     *
     * @param service the service containing the operation
     * @param operation the operation to invoke
     * @param pathParams path parameter values
     * @param queryParams query parameter values
     * @param body the request body (JSON string)
     * @return the prepared invocation request
     * @throws InvocationValidationException if validation fails
     */
    public InvocationRequest prepareInvocation(
            final DiscoveredService service,
            final Operation operation,
            final Map<String, String> pathParams,
            final Map<String, String> queryParams,
            final String body) {

        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");

        validateRequiredParameters(operation, pathParams, queryParams);

        final String url = buildUrl(service, operation, pathParams, queryParams);
        final Map<String, String> headers = Map.of(
            "Content-Type", "application/json",
            "Accept", "application/json"
        );

        return new InvocationRequest(url, operation.method(), body, headers);
    }

    private void validateRequiredParameters(
            final Operation operation,
            final Map<String, String> pathParams,
            final Map<String, String> queryParams) {

        final Map<String, String> safePathParams = pathParams != null ? pathParams : Map.of();
        final Map<String, String> safeQueryParams = queryParams != null ? queryParams : Map.of();

        for (final OperationParameter param : operation.parameters()) {
            if (!param.required()) {
                continue;
            }

            final boolean provided = switch (param.in()) {
                case PATH -> safePathParams.containsKey(param.name());
                case QUERY -> safeQueryParams.containsKey(param.name());
                default -> true;
            };

            if (!provided) {
                throw new InvocationValidationException(
                    "Required " + param.in().name().toLowerCase() + " parameter '"
                        + param.name() + "' is missing"
                );
            }
        }
    }

    private String buildUrl(
            final DiscoveredService service,
            final Operation operation,
            final Map<String, String> pathParams,
            final Map<String, String> queryParams) {

        String path = operation.path();

        // Substitute path parameters
        if (pathParams != null) {
            for (final Map.Entry<String, String> entry : pathParams.entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
            }
        }

        // Build query string
        final StringBuilder urlBuilder = new StringBuilder(service.address().toUrl(path));

        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (final Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(encode(entry.getKey()))
                    .append("=")
                    .append(encode(entry.getValue()));
                first = false;
            }
        }

        return urlBuilder.toString();
    }

    private String encode(final String value) {
        if (value == null) {
            return "";
        }
        // Simple URL encoding for common characters
        return value
            .replace(" ", "%20")
            .replace("&", "%26")
            .replace("=", "%3D")
            .replace("?", "%3F")
            .replace("#", "%23");
    }

    /**
     * Validates that an invocation result is acceptable.
     *
     * @param result the invocation result
     * @return true if the result indicates success
     */
    public boolean isSuccessful(final InvocationResult result) {
        return result != null && result.isSuccess();
    }

    /**
     * Exception thrown when invocation validation fails.
     */
    public static class InvocationValidationException extends RuntimeException {

        /**
         * Creates a new InvocationValidationException.
         *
         * @param message the error message
         */
        public InvocationValidationException(final String message) {
            super(message);
        }
    }
}
