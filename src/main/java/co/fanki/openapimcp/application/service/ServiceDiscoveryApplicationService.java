package co.fanki.openapimcp.application.service;

import co.fanki.openapimcp.application.query.GetOperationDetailsQuery;
import co.fanki.openapimcp.application.query.GetOperationsQuery;
import co.fanki.openapimcp.application.query.ListServicesQuery;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.ServiceStatus;
import co.fanki.openapimcp.domain.repository.DiscoveredServiceRepository;
import co.fanki.openapimcp.domain.service.OperationMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service for querying discovered services and their operations.
 *
 * <p>This service orchestrates the retrieval of service and operation data
 * from the domain layer, providing a clean interface for the MCP tools.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class ServiceDiscoveryApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(
        ServiceDiscoveryApplicationService.class
    );

    private final DiscoveredServiceRepository serviceRepository;
    private final OperationMatcher operationMatcher;

    /**
     * Creates a new ServiceDiscoveryApplicationService.
     *
     * @param serviceRepository the service repository
     * @param operationMatcher the operation matcher
     */
    public ServiceDiscoveryApplicationService(
            final DiscoveredServiceRepository serviceRepository,
            final OperationMatcher operationMatcher) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository);
        this.operationMatcher = Objects.requireNonNull(operationMatcher);
    }

    /**
     * Lists services based on the query criteria.
     *
     * @param query the list services query
     * @return list of matching services
     */
    public List<DiscoveredService> listServices(final ListServicesQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        LOG.debug("Listing services with query: {}", query);

        if (query.isActiveOnly()) {
            return serviceRepository.findActiveServices();
        }

        if (query.hasNamespaceFilter()) {
            final List<DiscoveredService> byNamespace = serviceRepository
                .findByNamespace(query.namespace());

            if (query.status() != null) {
                return byNamespace.stream()
                    .filter(s -> s.status() == query.status())
                    .toList();
            }
            return byNamespace;
        }

        if (query.status() != null) {
            return serviceRepository.findByStatus(query.status());
        }

        return serviceRepository.findAll();
    }

    /**
     * Gets operations from a service based on the query criteria.
     *
     * @param query the get operations query
     * @return list of matching operations
     */
    public List<Operation> getOperations(final GetOperationsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        LOG.debug("Getting operations with query: {}", query);

        final Optional<DiscoveredService> serviceOpt = serviceRepository
            .findById(query.serviceId());

        if (serviceOpt.isEmpty()) {
            LOG.warn("Service not found: {}", query.serviceId());
            return List.of();
        }

        final DiscoveredService service = serviceOpt.get();

        if (!service.hasSpecification()) {
            LOG.warn("Service has no OpenAPI spec: {}", query.serviceId());
            return List.of();
        }

        List<Operation> operations = service.allOperations();

        if (query.hasTagFilter()) {
            operations = operationMatcher.findByTag(service, query.tag());
        }

        if (query.hasMethodFilter()) {
            final String method = query.method().toUpperCase();
            operations = operations.stream()
                .filter(op -> method.equals(op.method()))
                .toList();
        }

        return operations;
    }

    /**
     * Gets detailed information about a specific operation.
     *
     * @param query the get operation details query
     * @return the operation details, or empty if not found
     */
    public Optional<OperationDetails> getOperationDetails(
            final GetOperationDetailsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        LOG.debug("Getting operation details with query: {}", query);

        final Optional<DiscoveredService> serviceOpt = serviceRepository
            .findById(query.serviceId());

        if (serviceOpt.isEmpty()) {
            return Optional.empty();
        }

        final DiscoveredService service = serviceOpt.get();
        final Optional<Operation> operationOpt = service.findOperation(query.operationId());

        if (operationOpt.isEmpty()) {
            return Optional.empty();
        }

        final Operation operation = operationOpt.get();
        final String endpointUrl = service.buildEndpointUrl(operation);

        return Optional.of(new OperationDetails(
            service,
            operation,
            endpointUrl
        ));
    }

    /**
     * Finds a service by its ID.
     *
     * @param serviceIdString the service ID as string
     * @return the service, or empty if not found
     */
    public Optional<DiscoveredService> findService(final String serviceIdString) {
        final var serviceId = co.fanki.openapimcp.domain.model.ServiceId
            .fromString(serviceIdString);
        return serviceRepository.findById(serviceId);
    }

    /**
     * Gets the count of services by status.
     *
     * @return status counts
     */
    public ServiceCounts getServiceCounts() {
        final int active = serviceRepository.findByStatus(ServiceStatus.ACTIVE).size();
        final int unreachable = serviceRepository.findByStatus(ServiceStatus.UNREACHABLE).size();
        final int noOpenApi = serviceRepository.findByStatus(ServiceStatus.NO_OPENAPI).size();

        return new ServiceCounts(active, unreachable, noOpenApi);
    }

    /**
     * Detailed information about an operation.
     *
     * @param service the owning service
     * @param operation the operation
     * @param endpointUrl the full endpoint URL
     */
    public record OperationDetails(
            DiscoveredService service,
            Operation operation,
            String endpointUrl) {

        public OperationDetails {
            Objects.requireNonNull(service);
            Objects.requireNonNull(operation);
            Objects.requireNonNull(endpointUrl);
        }
    }

    /**
     * Service counts by status.
     *
     * @param active count of active services
     * @param unreachable count of unreachable services
     * @param noOpenApi count of services without OpenAPI
     */
    public record ServiceCounts(int active, int unreachable, int noOpenApi) {

        public int total() {
            return active + unreachable + noOpenApi;
        }
    }
}
