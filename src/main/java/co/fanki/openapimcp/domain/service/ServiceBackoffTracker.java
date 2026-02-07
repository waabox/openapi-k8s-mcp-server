package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.ServiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks service failures and implements exponential backoff.
 *
 * <p>Services that fail repeatedly are temporarily skipped during refresh cycles
 * to avoid overwhelming failing endpoints and wasting resources.</p>
 *
 * <p>Backoff formula: baseBackoff * 2^(failureCount - 1), capped at maxBackoff.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class ServiceBackoffTracker {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceBackoffTracker.class);

    private final int maxFailures;
    private final Duration baseBackoff;
    private final Duration maxBackoff;
    private final Map<ServiceId, FailureRecord> failures;

    /**
     * Creates a new ServiceBackoffTracker.
     *
     * @param maxFailures number of consecutive failures before applying backoff
     * @param baseBackoffSeconds base backoff duration in seconds
     * @param maxBackoffSeconds maximum backoff duration in seconds
     */
    public ServiceBackoffTracker(
            final int maxFailures,
            final int baseBackoffSeconds,
            final int maxBackoffSeconds) {
        this.maxFailures = maxFailures > 0 ? maxFailures : 3;
        this.baseBackoff = Duration.ofSeconds(baseBackoffSeconds > 0 ? baseBackoffSeconds : 60);
        this.maxBackoff = Duration.ofSeconds(maxBackoffSeconds > 0 ? maxBackoffSeconds : 3600);
        this.failures = new ConcurrentHashMap<>();
    }

    /**
     * Records a failure for a service.
     *
     * @param serviceId the service that failed
     */
    public void recordFailure(final ServiceId serviceId) {
        failures.compute(serviceId, (id, record) -> {
            if (record == null) {
                return new FailureRecord(1, Instant.now());
            }
            return new FailureRecord(record.count + 1, Instant.now());
        });

        final FailureRecord record = failures.get(serviceId);
        if (record.count >= maxFailures) {
            final Duration backoff = calculateBackoff(record.count);
            LOG.warn("Service {} has failed {} times, backing off for {}",
                serviceId, record.count, backoff);
        }
    }

    /**
     * Records a success for a service, resetting its failure count.
     *
     * @param serviceId the service that succeeded
     */
    public void recordSuccess(final ServiceId serviceId) {
        final FailureRecord removed = failures.remove(serviceId);
        if (removed != null && removed.count >= maxFailures) {
            LOG.info("Service {} recovered after {} failures", serviceId, removed.count);
        }
    }

    /**
     * Checks if a service should be skipped due to backoff.
     *
     * @param serviceId the service to check
     * @return true if the service should be skipped
     */
    public boolean shouldSkip(final ServiceId serviceId) {
        final FailureRecord record = failures.get(serviceId);
        if (record == null || record.count < maxFailures) {
            return false;
        }

        final Duration backoff = calculateBackoff(record.count);
        final Instant backoffUntil = record.lastFailure.plus(backoff);

        if (Instant.now().isBefore(backoffUntil)) {
            LOG.debug("Skipping service {} (in backoff until {})", serviceId, backoffUntil);
            return true;
        }

        return false;
    }

    /**
     * Gets the current failure count for a service.
     *
     * @param serviceId the service to check
     * @return the failure count, or 0 if no failures
     */
    public int getFailureCount(final ServiceId serviceId) {
        final FailureRecord record = failures.get(serviceId);
        return record != null ? record.count : 0;
    }

    /**
     * Clears all failure records.
     */
    public void clear() {
        failures.clear();
    }

    private Duration calculateBackoff(final int failureCount) {
        final int exponent = Math.min(failureCount - maxFailures, 10);
        final long backoffSeconds = baseBackoff.toSeconds() * (1L << exponent);
        return Duration.ofSeconds(Math.min(backoffSeconds, maxBackoff.toSeconds()));
    }

    private record FailureRecord(int count, Instant lastFailure) {}
}
