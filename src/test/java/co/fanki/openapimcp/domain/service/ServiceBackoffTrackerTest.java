package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.ServiceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ServiceBackoffTracker}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class ServiceBackoffTrackerTest {

    @Test
    void whenRecordingFailure_givenNewService_shouldIncrementFailureCount() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);

        assertEquals(1, tracker.getFailureCount(serviceId));
    }

    @Test
    void whenRecordingMultipleFailures_givenService_shouldAccumulateCount() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);

        assertEquals(3, tracker.getFailureCount(serviceId));
    }

    @Test
    void whenRecordingSuccess_givenServiceWithFailures_shouldResetCount() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);
        tracker.recordSuccess(serviceId);

        assertEquals(0, tracker.getFailureCount(serviceId));
    }

    @Test
    void whenCheckingShouldSkip_givenLessThanMaxFailures_shouldReturnFalse() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);

        assertFalse(tracker.shouldSkip(serviceId));
    }

    @Test
    void whenCheckingShouldSkip_givenMaxFailuresReached_shouldReturnTrue() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 1, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);

        assertTrue(tracker.shouldSkip(serviceId));
    }

    @Test
    void whenCheckingShouldSkip_givenUnknownService_shouldReturnFalse() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "unknown-service");

        assertFalse(tracker.shouldSkip(serviceId));
    }

    @Test
    void whenGettingFailureCount_givenUnknownService_shouldReturnZero() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "unknown-service");

        assertEquals(0, tracker.getFailureCount(serviceId));
    }

    @Test
    void whenClearing_givenServicesWithFailures_shouldResetAll() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(3, 60, 3600);
        final ServiceId service1 = ServiceId.of("default", "service-1");
        final ServiceId service2 = ServiceId.of("default", "service-2");

        tracker.recordFailure(service1);
        tracker.recordFailure(service2);
        tracker.clear();

        assertEquals(0, tracker.getFailureCount(service1));
        assertEquals(0, tracker.getFailureCount(service2));
    }

    @Test
    void whenCreating_givenInvalidMaxFailures_shouldUseDefault() {
        final ServiceBackoffTracker tracker = new ServiceBackoffTracker(0, 60, 3600);
        final ServiceId serviceId = ServiceId.of("default", "my-service");

        tracker.recordFailure(serviceId);
        tracker.recordFailure(serviceId);

        assertFalse(tracker.shouldSkip(serviceId));

        tracker.recordFailure(serviceId);

        assertTrue(tracker.shouldSkip(serviceId));
    }
}
