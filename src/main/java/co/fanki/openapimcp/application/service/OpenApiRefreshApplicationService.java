package co.fanki.openapimcp.application.service;

import co.fanki.openapimcp.application.command.RefreshServicesCommand;
import co.fanki.openapimcp.domain.model.ClusterAddress;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.OpenApiPath;
import co.fanki.openapimcp.domain.model.OpenApiSpecification;
import co.fanki.openapimcp.domain.model.ServiceId;
import co.fanki.openapimcp.domain.repository.DiscoveredServiceRepository;
import co.fanki.openapimcp.domain.service.OpenApiParser;
import co.fanki.openapimcp.infrastructure.http.OpenApiFetcher;
import co.fanki.openapimcp.infrastructure.kubernetes.KubernetesServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service for refreshing OpenAPI specifications from Kubernetes services.
 *
 * <p>This service orchestrates the discovery of Kubernetes services and the
 * fetching of their OpenAPI specifications.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class OpenApiRefreshApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(
        OpenApiRefreshApplicationService.class
    );

    private final KubernetesServiceDiscovery kubernetesDiscovery;
    private final OpenApiFetcher openApiFetcher;
    private final OpenApiParser openApiParser;
    private final DiscoveredServiceRepository serviceRepository;

    /**
     * Creates a new OpenApiRefreshApplicationService.
     *
     * @param kubernetesDiscovery the Kubernetes discovery service
     * @param openApiFetcher the OpenAPI fetcher
     * @param openApiParser the OpenAPI parser
     * @param serviceRepository the service repository
     */
    public OpenApiRefreshApplicationService(
            final KubernetesServiceDiscovery kubernetesDiscovery,
            final OpenApiFetcher openApiFetcher,
            final OpenApiParser openApiParser,
            final DiscoveredServiceRepository serviceRepository) {
        this.kubernetesDiscovery = Objects.requireNonNull(kubernetesDiscovery);
        this.openApiFetcher = Objects.requireNonNull(openApiFetcher);
        this.openApiParser = Objects.requireNonNull(openApiParser);
        this.serviceRepository = Objects.requireNonNull(serviceRepository);
    }

    /**
     * Refreshes all services based on the command.
     *
     * @param command the refresh command
     * @return the refresh result
     */
    public RefreshResult refresh(final RefreshServicesCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        LOG.info("Starting service refresh: {}", command);

        final List<KubernetesServiceDiscovery.DiscoveredK8sService> k8sServices =
            kubernetesDiscovery.discoverServices();

        LOG.info("Discovered {} Kubernetes services", k8sServices.size());

        int created = 0;
        int updated = 0;
        int failed = 0;
        final List<ServiceId> processedIds = new ArrayList<>();

        for (final var k8sService : k8sServices) {
            if (!command.includesNamespace(k8sService.namespace())) {
                continue;
            }

            final ServiceId serviceId = ServiceId.of(
                k8sService.namespace(),
                k8sService.name()
            );
            processedIds.add(serviceId);

            try {
                final boolean isNew = processService(k8sService);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (final Exception e) {
                LOG.error("Failed to process service {}: {}", serviceId, e.getMessage());
                failed++;
            }
        }

        // Remove stale services
        if (!processedIds.isEmpty()) {
            serviceRepository.deleteNotIn(processedIds);
        }

        final RefreshResult result = new RefreshResult(
            created, updated, failed, processedIds.size()
        );

        LOG.info("Refresh completed: {}", result);
        return result;
    }

    private boolean processService(
            final KubernetesServiceDiscovery.DiscoveredK8sService k8sService) {

        final ServiceId serviceId = ServiceId.of(
            k8sService.namespace(),
            k8sService.name()
        );

        final ClusterAddress address = ClusterAddress.of(
            k8sService.clusterIp(),
            k8sService.port()
        );

        final OpenApiPath openApiPath = OpenApiPath.of(k8sService.openApiPath());

        final Optional<DiscoveredService> existingOpt = serviceRepository.findById(serviceId);
        final boolean isNew = existingOpt.isEmpty();

        final DiscoveredService service = existingOpt.orElseGet(() ->
            DiscoveredService.discover(serviceId, address, openApiPath)
        );

        // Fetch OpenAPI specification
        final String openApiUrl = address.toUrl(openApiPath.value());
        LOG.debug("Fetching OpenAPI from: {}", openApiUrl);

        final Optional<String> specJsonOpt = openApiFetcher.fetch(openApiUrl);

        if (specJsonOpt.isEmpty()) {
            LOG.warn("No OpenAPI spec found for service: {}", serviceId);
            service.markNoOpenApi();
        } else {
            try {
                final OpenApiSpecification spec = openApiParser.parse(specJsonOpt.get());
                service.attachSpecification(spec);
                LOG.info("Attached OpenAPI spec to service {}: {} operations",
                    serviceId, spec.operationCount());
            } catch (final OpenApiParser.OpenApiParseException e) {
                LOG.error("Failed to parse OpenAPI for {}: {}", serviceId, e.getMessage());
                service.markNoOpenApi();
            }
        }

        serviceRepository.save(service);
        return isNew;
    }

    /**
     * Refreshes a single service by ID.
     *
     * @param serviceId the service ID
     * @return true if refresh was successful
     */
    public boolean refreshService(final ServiceId serviceId) {
        Objects.requireNonNull(serviceId, "serviceId cannot be null");

        final Optional<DiscoveredService> serviceOpt = serviceRepository.findById(serviceId);

        if (serviceOpt.isEmpty()) {
            LOG.warn("Service not found for refresh: {}", serviceId);
            return false;
        }

        final DiscoveredService service = serviceOpt.get();
        final String openApiUrl = service.openApiUrl();

        LOG.debug("Refreshing single service {} from: {}", serviceId, openApiUrl);

        final Optional<String> specJsonOpt = openApiFetcher.fetch(openApiUrl);

        if (specJsonOpt.isEmpty()) {
            service.markUnreachable();
            serviceRepository.save(service);
            return false;
        }

        try {
            final OpenApiSpecification spec = openApiParser.parse(specJsonOpt.get());
            service.attachSpecification(spec);
            serviceRepository.save(service);
            return true;
        } catch (final OpenApiParser.OpenApiParseException e) {
            LOG.error("Failed to parse OpenAPI for {}: {}", serviceId, e.getMessage());
            service.markNoOpenApi();
            serviceRepository.save(service);
            return false;
        }
    }

    /**
     * Result of a refresh operation.
     *
     * @param created number of new services created
     * @param updated number of services updated
     * @param failed number of services that failed to process
     * @param total total number of services processed
     */
    public record RefreshResult(int created, int updated, int failed, int total) {

        @Override
        public String toString() {
            return String.format(
                "RefreshResult{total=%d, created=%d, updated=%d, failed=%d}",
                total, created, updated, failed
            );
        }
    }
}
