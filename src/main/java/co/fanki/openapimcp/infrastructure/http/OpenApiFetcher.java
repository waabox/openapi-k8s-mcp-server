package co.fanki.openapimcp.infrastructure.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * HTTP client for fetching OpenAPI specifications from services.
 *
 * <p>Uses WebClient for non-blocking HTTP requests with retry support.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class OpenApiFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiFetcher.class);

    private final WebClient webClient;
    private final Semaphore rateLimiter;

    @Value("${openapi.fetch.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${openapi.fetch.retry-attempts:3}")
    private int retryAttempts;

    /**
     * Creates a new OpenApiFetcher.
     *
     * @param webClientBuilder the WebClient builder
     * @param maxConcurrentRequests maximum number of concurrent requests
     */
    public OpenApiFetcher(
            final WebClient.Builder webClientBuilder,
            @Value("${openapi.fetch.max-concurrent-requests:10}") final int maxConcurrentRequests) {
        this.webClient = Objects.requireNonNull(webClientBuilder)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB max
            .build();
        this.rateLimiter = new Semaphore(maxConcurrentRequests > 0 ? maxConcurrentRequests : 10);
        LOG.info("OpenApiFetcher initialized with max {} concurrent requests", maxConcurrentRequests);
    }

    /**
     * Fetches an OpenAPI specification from the given URL.
     *
     * @param url the URL to fetch from
     * @return the JSON specification, or empty if fetch failed
     */
    public Optional<String> fetch(final String url) {
        Objects.requireNonNull(url, "url cannot be null");

        LOG.debug("Fetching OpenAPI spec from: {}", url);

        try {
            rateLimiter.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for rate limiter: {}", url);
            return Optional.empty();
        }

        try {
            final String response = webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    LOG.warn("Error response from {}: {}", url, clientResponse.statusCode());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(retryAttempts, Duration.ofSeconds(1))
                    .filter(this::isRetryable)
                    .doBeforeRetry(signal ->
                        LOG.debug("Retrying fetch for {}, attempt {}",
                            url, signal.totalRetries() + 1)))
                .onErrorResume(e -> {
                    LOG.warn("Failed to fetch OpenAPI from {}: {}", url, e.getMessage());
                    return Mono.empty();
                })
                .block();

            if (response != null && !response.isBlank()) {
                LOG.debug("Successfully fetched OpenAPI spec from {} ({} bytes)",
                    url, response.length());
                return Optional.of(response);
            }

            return Optional.empty();

        } catch (final Exception e) {
            LOG.error("Exception fetching OpenAPI from {}: {}", url, e.getMessage());
            return Optional.empty();
        } finally {
            rateLimiter.release();
        }
    }

    /**
     * Checks if an OpenAPI endpoint is available.
     *
     * @param url the URL to check
     * @return true if the endpoint returns a valid response
     */
    public boolean isAvailable(final String url) {
        Objects.requireNonNull(url, "url cannot be null");

        try {
            final Boolean available = webClient.head()
                .uri(url)
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(false)
                .block();

            return Boolean.TRUE.equals(available);

        } catch (final Exception e) {
            LOG.debug("Availability check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private boolean isRetryable(final Throwable throwable) {
        // Retry on connection errors, not on 4xx/5xx responses
        return throwable instanceof java.net.ConnectException
            || throwable instanceof java.net.SocketTimeoutException
            || throwable instanceof java.io.IOException;
    }
}
