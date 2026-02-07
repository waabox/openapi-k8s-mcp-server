package co.fanki.openapimcp.infrastructure.scheduling;

import co.fanki.openapimcp.application.command.RefreshServicesCommand;
import co.fanki.openapimcp.application.service.OpenApiRefreshApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler for periodic refresh of OpenAPI specifications.
 *
 * <p>Runs at configurable intervals (default 10 minutes) to discover
 * new services and refresh existing OpenAPI specifications.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class RefreshScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshScheduler.class);

    private final OpenApiRefreshApplicationService refreshService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${scheduler.refresh.enabled:true}")
    private boolean enabled;

    /**
     * Creates a new RefreshScheduler.
     *
     * @param refreshService the refresh service
     */
    public RefreshScheduler(final OpenApiRefreshApplicationService refreshService) {
        this.refreshService = Objects.requireNonNull(refreshService);
    }

    /**
     * Performs initial refresh when the application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!enabled) {
            LOG.info("Scheduler is disabled, skipping initial refresh");
            return;
        }

        LOG.info("Performing initial service discovery...");
        executeRefresh();
    }

    /**
     * Scheduled refresh task - runs every 10 minutes by default.
     */
    @Scheduled(fixedRateString = "${scheduler.refresh.interval-ms:600000}")
    public void scheduledRefresh() {
        if (!enabled) {
            return;
        }

        LOG.debug("Scheduled refresh triggered");
        executeRefresh();
    }

    /**
     * Manually triggers a refresh.
     *
     * @return the refresh result
     */
    public OpenApiRefreshApplicationService.RefreshResult triggerRefresh() {
        LOG.info("Manual refresh triggered");
        return executeRefresh();
    }

    /**
     * Manually triggers a forced refresh (ignores caching).
     *
     * @return the refresh result
     */
    public OpenApiRefreshApplicationService.RefreshResult triggerForcedRefresh() {
        LOG.info("Forced refresh triggered");

        if (!running.compareAndSet(false, true)) {
            LOG.warn("Refresh already in progress, skipping");
            return new OpenApiRefreshApplicationService.RefreshResult(0, 0, 0, 0, 0);
        }

        try {
            return refreshService.refresh(RefreshServicesCommand.forced());
        } finally {
            running.set(false);
        }
    }

    private OpenApiRefreshApplicationService.RefreshResult executeRefresh() {
        if (!running.compareAndSet(false, true)) {
            LOG.warn("Refresh already in progress, skipping");
            return new OpenApiRefreshApplicationService.RefreshResult(0, 0, 0, 0, 0);
        }

        try {
            return refreshService.refresh(RefreshServicesCommand.all());
        } catch (final Exception e) {
            LOG.error("Refresh failed: {}", e.getMessage(), e);
            return new OpenApiRefreshApplicationService.RefreshResult(0, 0, 1, 0, 0);
        } finally {
            running.set(false);
        }
    }

    /**
     * Checks if a refresh is currently running.
     *
     * @return true if refresh is in progress
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Checks if the scheduler is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
