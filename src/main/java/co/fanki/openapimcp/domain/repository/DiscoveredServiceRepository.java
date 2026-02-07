package co.fanki.openapimcp.domain.repository;

import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.OpenApiSpecification;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.ServiceId;
import co.fanki.openapimcp.domain.model.ServiceStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository for managing {@link DiscoveredService} aggregates.
 *
 * <p>This is a concrete class using JDBI for database operations.
 * The repository manages both the discovered_services and openapi_specifications
 * tables as part of the aggregate boundary.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Repository
public class DiscoveredServiceRepository {

    private static final Logger LOG = LoggerFactory.getLogger(
        DiscoveredServiceRepository.class
    );

    private final Jdbi jdbi;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new DiscoveredServiceRepository.
     *
     * @param jdbi the JDBI instance
     * @param objectMapper the JSON object mapper for serialization
     */
    public DiscoveredServiceRepository(
            final Jdbi jdbi,
            final ObjectMapper objectMapper) {
        this.jdbi = Objects.requireNonNull(jdbi);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Saves or updates a discovered service.
     *
     * <p>If the service already exists, it will be updated.
     * The associated OpenAPI specification is also persisted.</p>
     *
     * @param service the service to save
     * @return the saved service
     */
    public DiscoveredService save(final DiscoveredService service) {
        Objects.requireNonNull(service, "service cannot be null");

        return jdbi.inTransaction(handle -> {
            final String serviceIdStr = service.id().asString();

            // Check if service exists
            final boolean exists = handle.createQuery(
                "SELECT COUNT(*) FROM discovered_services WHERE id = :id"
            )
                .bind("id", serviceIdStr)
                .mapTo(Integer.class)
                .one() > 0;

            if (exists) {
                // Update existing service
                handle.createUpdate(
                    "UPDATE discovered_services SET "
                    + "cluster_ip = :clusterIp, "
                    + "cluster_port = :clusterPort, "
                    + "openapi_path = :openApiPath, "
                    + "status = :status, "
                    + "last_checked_at = :lastCheckedAt "
                    + "WHERE id = :id"
                )
                    .bind("id", serviceIdStr)
                    .bind("clusterIp", service.address().ip())
                    .bind("clusterPort", service.address().port())
                    .bind("openApiPath", service.openApiPath().value())
                    .bind("status", service.status().name())
                    .bind("lastCheckedAt", toTimestamp(service.lastCheckedAt()))
                    .execute();
            } else {
                // Insert new service
                handle.createUpdate(
                    "INSERT INTO discovered_services "
                    + "(id, namespace, name, cluster_ip, cluster_port, "
                    + "openapi_path, status, discovered_at, last_checked_at) "
                    + "VALUES (:id, :namespace, :name, :clusterIp, :clusterPort, "
                    + ":openApiPath, :status, :discoveredAt, :lastCheckedAt)"
                )
                    .bind("id", serviceIdStr)
                    .bind("namespace", service.id().namespace())
                    .bind("name", service.id().name())
                    .bind("clusterIp", service.address().ip())
                    .bind("clusterPort", service.address().port())
                    .bind("openApiPath", service.openApiPath().value())
                    .bind("status", service.status().name())
                    .bind("discoveredAt", toTimestamp(service.discoveredAt()))
                    .bind("lastCheckedAt", toTimestamp(service.lastCheckedAt()))
                    .execute();
            }

            // Save specification if present
            service.specification().ifPresent(spec ->
                saveSpecification(handle, serviceIdStr, spec)
            );

            return service;
        });
    }

    private void saveSpecification(
            final org.jdbi.v3.core.Handle handle,
            final String serviceId,
            final OpenApiSpecification spec) {

        // Delete existing specification
        handle.createUpdate(
            "DELETE FROM openapi_specifications WHERE service_id = :serviceId"
        )
            .bind("serviceId", serviceId)
            .execute();

        // Insert new specification
        final String operationsJson = serializeOperations(spec.operations());

        handle.createUpdate(
            "INSERT INTO openapi_specifications "
            + "(service_id, title, version, raw_json, operations_json, fetched_at) "
            + "VALUES (:serviceId, :title, :version, :rawJson, :operationsJson, :fetchedAt)"
        )
            .bind("serviceId", serviceId)
            .bind("title", spec.title())
            .bind("version", spec.version())
            .bind("rawJson", spec.rawJson())
            .bind("operationsJson", operationsJson)
            .bind("fetchedAt", toTimestamp(spec.fetchedAt()))
            .execute();
    }

    /**
     * Finds a service by its identifier.
     *
     * @param id the service identifier
     * @return the service with its specification, or empty if not found
     */
    public Optional<DiscoveredService> findById(final ServiceId id) {
        Objects.requireNonNull(id, "id cannot be null");

        return jdbi.withHandle(handle -> {
            final Optional<DiscoveredService> serviceOpt = handle.createQuery(
                "SELECT * FROM discovered_services WHERE id = :id"
            )
                .bind("id", id.asString())
                .map(new DiscoveredServiceRowMapper())
                .findFirst();

            return serviceOpt.map(service -> attachSpecification(handle, service));
        });
    }

    /**
     * Finds all discovered services.
     *
     * @return list of all services with their specifications
     */
    public List<DiscoveredService> findAll() {
        return jdbi.withHandle(handle -> {
            final List<DiscoveredService> services = handle.createQuery(
                "SELECT * FROM discovered_services ORDER BY namespace, name"
            )
                .map(new DiscoveredServiceRowMapper())
                .list();

            return services.stream()
                .map(service -> attachSpecification(handle, service))
                .toList();
        });
    }

    /**
     * Finds services by namespace.
     *
     * @param namespace the Kubernetes namespace
     * @return list of services in the namespace with their specifications
     */
    public List<DiscoveredService> findByNamespace(final String namespace) {
        Objects.requireNonNull(namespace, "namespace cannot be null");

        return jdbi.withHandle(handle -> {
            final List<DiscoveredService> services = handle.createQuery(
                "SELECT * FROM discovered_services WHERE namespace = :namespace "
                + "ORDER BY name"
            )
                .bind("namespace", namespace)
                .map(new DiscoveredServiceRowMapper())
                .list();

            return services.stream()
                .map(service -> attachSpecification(handle, service))
                .toList();
        });
    }

    /**
     * Finds services by status.
     *
     * @param status the service status
     * @return list of services with the given status and their specifications
     */
    public List<DiscoveredService> findByStatus(final ServiceStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return jdbi.withHandle(handle -> {
            final List<DiscoveredService> services = handle.createQuery(
                "SELECT * FROM discovered_services WHERE status = :status "
                + "ORDER BY namespace, name"
            )
                .bind("status", status.name())
                .map(new DiscoveredServiceRowMapper())
                .list();

            return services.stream()
                .map(service -> attachSpecification(handle, service))
                .toList();
        });
    }

    /**
     * Finds all active services (services with valid OpenAPI specs).
     *
     * @return list of active services with their specifications
     */
    public List<DiscoveredService> findActiveServices() {
        return findByStatus(ServiceStatus.ACTIVE);
    }

    /**
     * Deletes a service by its identifier.
     *
     * <p>The associated specification is also deleted due to foreign key cascade.</p>
     *
     * @param id the service identifier
     */
    public void deleteById(final ServiceId id) {
        Objects.requireNonNull(id, "id cannot be null");

        jdbi.useHandle(handle ->
            handle.createUpdate("DELETE FROM discovered_services WHERE id = :id")
                .bind("id", id.asString())
                .execute()
        );
    }

    /**
     * Deletes services not in the provided list of IDs.
     *
     * <p>Used during refresh to remove stale services that are no longer
     * present in the Kubernetes cluster.</p>
     *
     * @param activeIds the IDs of currently active services
     */
    public void deleteNotIn(final List<ServiceId> activeIds) {
        if (activeIds == null || activeIds.isEmpty()) {
            // Delete all if no active IDs provided
            jdbi.useHandle(handle ->
                handle.createUpdate("DELETE FROM discovered_services").execute()
            );
            return;
        }

        final List<String> activeIdStrings = activeIds.stream()
            .map(ServiceId::asString)
            .toList();

        jdbi.useHandle(handle -> {
            // Derby doesn't support IN with parameters easily, so we delete one by one
            // or build a dynamic query
            final String placeholders = String.join(", ",
                activeIdStrings.stream().map(id -> "'" + id.replace("'", "''") + "'").toList()
            );

            handle.createUpdate(
                "DELETE FROM discovered_services WHERE id NOT IN (" + placeholders + ")"
            ).execute();
        });
    }

    /**
     * Checks if a service exists.
     *
     * @param id the service identifier
     * @return true if the service exists
     */
    public boolean exists(final ServiceId id) {
        Objects.requireNonNull(id, "id cannot be null");

        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT COUNT(*) FROM discovered_services WHERE id = :id"
            )
                .bind("id", id.asString())
                .mapTo(Integer.class)
                .one() > 0
        );
    }

    private DiscoveredService attachSpecification(
            final org.jdbi.v3.core.Handle handle,
            final DiscoveredService service) {

        final Optional<OpenApiSpecification> specOpt = handle.createQuery(
            "SELECT * FROM openapi_specifications WHERE service_id = :serviceId"
        )
            .bind("serviceId", service.id().asString())
            .map(new OpenApiSpecificationRowMapper(objectMapper))
            .findFirst();

        if (specOpt.isPresent()) {
            return DiscoveredService.reconstitute(
                service.id(),
                service.address(),
                service.openApiPath(),
                service.discoveredAt(),
                service.status(),
                service.lastCheckedAt(),
                specOpt.get()
            );
        }

        return service;
    }

    private String serializeOperations(final List<Operation> operations) {
        if (operations == null || operations.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(operations);
        } catch (final JsonProcessingException e) {
            LOG.error("Failed to serialize operations", e);
            return "[]";
        }
    }

    private Timestamp toTimestamp(final LocalDateTime dateTime) {
        return dateTime != null ? Timestamp.valueOf(dateTime) : null;
    }
}
