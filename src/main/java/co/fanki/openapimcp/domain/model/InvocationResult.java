package co.fanki.openapimcp.domain.model;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Value Object representing the result of invoking an HTTP endpoint.
 *
 * <p>Contains the status code, response body, headers, and timing information.</p>
 *
 * <p>This is an immutable value object.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class InvocationResult {

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;
    private final Duration duration;
    private final String errorMessage;

    private InvocationResult(
            final int statusCode,
            final String body,
            final Map<String, String> headers,
            final Duration duration,
            final String errorMessage) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.duration = duration;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful invocation result.
     *
     * @param statusCode the HTTP status code
     * @param body the response body
     * @param headers the response headers
     * @param duration the request duration
     * @return a new InvocationResult instance
     */
    public static InvocationResult success(
            final int statusCode,
            final String body,
            final Map<String, String> headers,
            final Duration duration) {
        return new InvocationResult(statusCode, body, headers, duration, null);
    }

    /**
     * Creates a failed invocation result.
     *
     * @param errorMessage the error message
     * @param duration the duration until failure
     * @return a new InvocationResult instance
     */
    public static InvocationResult failure(final String errorMessage, final Duration duration) {
        return new InvocationResult(-1, null, null, duration, errorMessage);
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code, or -1 if the request failed
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the response body.
     *
     * @return the body, may be null
     */
    public String body() {
        return body;
    }

    /**
     * Returns the response headers.
     *
     * @return immutable map of headers
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Returns the request duration.
     *
     * @return the duration
     */
    public Duration duration() {
        return duration;
    }

    /**
     * Returns the error message if the invocation failed.
     *
     * @return the error message, or null if successful
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Checks if the invocation was successful (2xx status code).
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Checks if the invocation failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return errorMessage != null || statusCode < 0;
    }

    /**
     * Checks if the response is a client error (4xx).
     *
     * @return true if client error
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Checks if the response is a server error (5xx).
     *
     * @return true if server error
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Returns the duration in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long durationMs() {
        return duration == null ? 0 : duration.toMillis();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InvocationResult that = (InvocationResult) o;
        return statusCode == that.statusCode
            && Objects.equals(body, that.body)
            && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, body, errorMessage);
    }

    @Override
    public String toString() {
        if (isFailure()) {
            return "InvocationResult{error='" + errorMessage + "'}";
        }
        return "InvocationResult{statusCode=" + statusCode + ", durationMs=" + durationMs() + "}";
    }
}
