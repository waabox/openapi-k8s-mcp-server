package co.fanki.openapimcp.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Root representing a discovered microservice in the Kubernetes cluster.
 *
 * <p>This is the main entity in the domain. It owns the {@link OpenApiSpecification}
 * as part of its aggregate boundary. All operations on the specification must go
 * through this aggregate root.</p>
 *
 * <p>The service lifecycle is managed through state transitions:
 * <ul>
 *   <li>ACTIVE: Service has a valid OpenAPI specification</li>
 *   <li>UNREACHABLE: Service cannot be reached</li>
 *   <li>NO_OPENAPI: Service is reachable but has no OpenAPI spec</li>
 * </ul>
 * </p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public class DiscoveredService {

    private final ServiceId id;
    private final ClusterAddress address;
    private final OpenApiPath openApiPath;
    private final LocalDateTime discoveredAt;

    private OpenApiSpecification specification;
    private ServiceStatus status;
    private LocalDateTime lastCheckedAt;

    private DiscoveredService(
            final ServiceId id,
            final ClusterAddress address,
            final OpenApiPath openApiPath,
            final LocalDateTime discoveredAt,
            final ServiceStatus status,
            final LocalDateTime lastCheckedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.address = Objects.requireNonNull(address, "address cannot be null");
        this.openApiPath = openApiPath != null ? openApiPath : OpenApiPath.defaultPath();
        this.discoveredAt = discoveredAt != null ? discoveredAt : LocalDateTime.now();
        this.status = status != null ? status : ServiceStatus.ACTIVE;
        this.lastCheckedAt = lastCheckedAt;
    }

    /**
     * Discovers a new service in the cluster.
     *
     * @param id the service identifier
     * @param address the cluster address
     * @param openApiPath the path to the OpenAPI specification
     * @return a new DiscoveredService instance
     */
    public static DiscoveredService discover(
            final ServiceId id,
            final ClusterAddress address,
            final OpenApiPath openApiPath) {
        return new DiscoveredService(
            id,
            address,
            openApiPath,
            LocalDateTime.now(),
            ServiceStatus.ACTIVE,
            null
        );
    }

    /**
     * Reconstructs a DiscoveredService from persisted data.
     *
     * @param id the service identifier
     * @param address the cluster address
     * @param openApiPath the OpenAPI path
     * @param discoveredAt when the service was discovered
     * @param status the current status
     * @param lastCheckedAt when the service was last checked
     * @param specification the OpenAPI specification (optional)
     * @return a reconstructed DiscoveredService instance
     */
    public static DiscoveredService reconstitute(
            final ServiceId id,
            final ClusterAddress address,
            final OpenApiPath openApiPath,
            final LocalDateTime discoveredAt,
            final ServiceStatus status,
            final LocalDateTime lastCheckedAt,
            final OpenApiSpecification specification) {
        final DiscoveredService service = new DiscoveredService(
            id, address, openApiPath, discoveredAt, status, lastCheckedAt
        );
        service.specification = specification;
        return service;
    }

    /**
     * Attaches an OpenAPI specification to this service.
     *
     * <p>This marks the service as ACTIVE.</p>
     *
     * @param spec the specification to attach
     */
    public void attachSpecification(final OpenApiSpecification spec) {
        this.specification = Objects.requireNonNull(spec, "spec cannot be null");
        this.status = ServiceStatus.ACTIVE;
        this.lastCheckedAt = LocalDateTime.now();
    }

    /**
     * Marks this service as unreachable.
     *
     * <p>Called when the service cannot be contacted during refresh.</p>
     */
    public void markUnreachable() {
        this.status = ServiceStatus.UNREACHABLE;
        this.lastCheckedAt = LocalDateTime.now();
    }

    /**
     * Marks this service as having no OpenAPI specification.
     *
     * <p>Called when the service is reachable but does not expose an OpenAPI spec.</p>
     */
    public void markNoOpenApi() {
        this.status = ServiceStatus.NO_OPENAPI;
        this.specification = null;
        this.lastCheckedAt = LocalDateTime.now();
    }

    /**
     * Returns the service identifier.
     *
     * @return the service ID
     */
    public ServiceId id() {
        return id;
    }

    /**
     * Returns the cluster address.
     *
     * @return the address
     */
    public ClusterAddress address() {
        return address;
    }

    /**
     * Returns the OpenAPI specification path.
     *
     * @return the OpenAPI path
     */
    public OpenApiPath openApiPath() {
        return openApiPath;
    }

    /**
     * Returns when this service was discovered.
     *
     * @return the discovery timestamp
     */
    public LocalDateTime discoveredAt() {
        return discoveredAt;
    }

    /**
     * Returns the current service status.
     *
     * @return the status
     */
    public ServiceStatus status() {
        return status;
    }

    /**
     * Returns when this service was last checked.
     *
     * @return the last check timestamp, may be null
     */
    public LocalDateTime lastCheckedAt() {
        return lastCheckedAt;
    }

    /**
     * Returns the OpenAPI specification, if available.
     *
     * @return the specification, or empty if not available
     */
    public Optional<OpenApiSpecification> specification() {
        return Optional.ofNullable(specification);
    }

    /**
     * Checks if this service has an OpenAPI specification.
     *
     * @return true if specification is available
     */
    public boolean hasSpecification() {
        return specification != null;
    }

    /**
     * Checks if this service is currently active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }

    /**
     * Finds an operation by its unique identifier.
     *
     * @param operationId the operation ID
     * @return the operation, or empty if not found
     */
    public Optional<Operation> findOperation(final String operationId) {
        if (specification == null) {
            return Optional.empty();
        }
        return specification.findOperation(operationId);
    }

    /**
     * Returns all operations, optionally filtered by tag.
     *
     * @param tag the tag to filter by, or null for all operations
     * @return list of operations
     */
    public List<Operation> operations(final String tag) {
        if (specification == null) {
            return List.of();
        }
        return specification.operationsByTag(tag);
    }

    /**
     * Returns all operations in this service.
     *
     * @return list of all operations
     */
    public List<Operation> allOperations() {
        return operations(null);
    }

    /**
     * Builds the full URL for invoking an operation.
     *
     * @param operation the operation to invoke
     * @return the full endpoint URL
     */
    public String buildEndpointUrl(final Operation operation) {
        return address.toUrl(operation.path());
    }

    /**
     * Returns the URL for fetching the OpenAPI specification.
     *
     * @return the OpenAPI spec URL
     */
    public String openApiUrl() {
        return address.toUrl(openApiPath.value());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DiscoveredService that = (DiscoveredService) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DiscoveredService{"
            + "id=" + id
            + ", status=" + status
            + ", hasSpec=" + hasSpecification()
            + '}';
    }
}
