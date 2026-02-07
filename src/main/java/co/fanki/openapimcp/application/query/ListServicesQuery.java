package co.fanki.openapimcp.application.query;

import co.fanki.openapimcp.domain.model.ServiceStatus;

/**
 * Query to list discovered services.
 *
 * <p>Supports filtering by namespace and status.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class ListServicesQuery {

    private final String namespace;
    private final ServiceStatus status;
    private final boolean activeOnly;

    private ListServicesQuery(
            final String namespace,
            final ServiceStatus status,
            final boolean activeOnly) {
        this.namespace = namespace;
        this.status = status;
        this.activeOnly = activeOnly;
    }

    /**
     * Creates a query to list all services.
     *
     * @return a new ListServicesQuery
     */
    public static ListServicesQuery all() {
        return new ListServicesQuery(null, null, false);
    }

    /**
     * Creates a query to list services in a specific namespace.
     *
     * @param namespace the namespace to filter by
     * @return a new ListServicesQuery
     */
    public static ListServicesQuery byNamespace(final String namespace) {
        return new ListServicesQuery(namespace, null, false);
    }

    /**
     * Creates a query to list only active services.
     *
     * @return a new ListServicesQuery
     */
    public static ListServicesQuery activeOnly() {
        return new ListServicesQuery(null, ServiceStatus.ACTIVE, true);
    }

    /**
     * Creates a query to list services with a specific status.
     *
     * @param status the status to filter by
     * @return a new ListServicesQuery
     */
    public static ListServicesQuery byStatus(final ServiceStatus status) {
        return new ListServicesQuery(null, status, false);
    }

    /**
     * Returns the namespace filter.
     *
     * @return the namespace, or null for all
     */
    public String namespace() {
        return namespace;
    }

    /**
     * Returns the status filter.
     *
     * @return the status, or null for all
     */
    public ServiceStatus status() {
        return status;
    }

    /**
     * Checks if only active services should be returned.
     *
     * @return true if active only
     */
    public boolean isActiveOnly() {
        return activeOnly;
    }

    /**
     * Checks if this query has a namespace filter.
     *
     * @return true if filtered by namespace
     */
    public boolean hasNamespaceFilter() {
        return namespace != null && !namespace.isBlank();
    }

    @Override
    public String toString() {
        return "ListServicesQuery{"
            + "namespace='" + namespace + '\''
            + ", status=" + status
            + ", activeOnly=" + activeOnly
            + '}';
    }
}
