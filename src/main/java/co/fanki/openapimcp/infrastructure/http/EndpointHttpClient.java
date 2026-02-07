package co.fanki.openapimcp.infrastructure.http;

import co.fanki.openapimcp.domain.model.InvocationResult;
import co.fanki.openapimcp.domain.service.EndpointInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP client for invoking microservice endpoints.
 *
 * <p>Uses WebClient for non-blocking HTTP requests and returns
 * standardized InvocationResult objects.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class EndpointHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointHttpClient.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    /**
     * Creates a new EndpointHttpClient.
     *
     * @param webClientBuilder the WebClient builder
     */
    public EndpointHttpClient(final WebClient.Builder webClientBuilder) {
        this.webClient = Objects.requireNonNull(webClientBuilder)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    /**
     * Invokes an endpoint based on the prepared request.
     *
     * @param request the invocation request
     * @return the invocation result
     */
    public InvocationResult invoke(final EndpointInvoker.InvocationRequest request) {
        Objects.requireNonNull(request, "request cannot be null");

        final Instant start = Instant.now();

        try {
            final HttpMethod method = HttpMethod.valueOf(request.method());

            final WebClient.RequestBodySpec requestSpec = webClient
                .method(method)
                .uri(request.url());

            // Add headers
            request.headers().forEach(requestSpec::header);

            // Add body if present
            final WebClient.RequestHeadersSpec<?> finalSpec;
            if (request.body() != null && !request.body().isBlank()) {
                finalSpec = requestSpec.bodyValue(request.body());
            } else {
                finalSpec = requestSpec;
            }

            // Execute request
            final ResponseData responseData = finalSpec
                .exchangeToMono(this::extractResponse)
                .timeout(DEFAULT_TIMEOUT)
                .block();

            final Duration duration = Duration.between(start, Instant.now());

            if (responseData == null) {
                return InvocationResult.failure("No response received", duration);
            }

            return InvocationResult.success(
                responseData.statusCode(),
                responseData.body(),
                responseData.headers(),
                duration
            );

        } catch (final Exception e) {
            final Duration duration = Duration.between(start, Instant.now());
            LOG.error("Invocation failed for {} {}: {}",
                request.method(), request.url(), e.getMessage());
            return InvocationResult.failure(e.getMessage(), duration);
        }
    }

    private Mono<ResponseData> extractResponse(final ClientResponse response) {
        final int statusCode = response.statusCode().value();
        final Map<String, String> headers = extractHeaders(response);

        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ResponseData(statusCode, body, headers));
    }

    private Map<String, String> extractHeaders(final ClientResponse response) {
        final Map<String, String> headers = new HashMap<>();

        response.headers().asHttpHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });

        return headers;
    }

    /**
     * Internal record to hold response data.
     */
    private record ResponseData(
            int statusCode,
            String body,
            Map<String, String> headers) {
    }
}
