package co.fanki.openapimcp.application.query;

import co.fanki.openapimcp.domain.model.ServiceId;

import java.util.Objects;

/**
 * Query to get operations from a service.
 *
 * <p>Supports filtering by tag and HTTP method.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class GetOperationsQuery {

    private final ServiceId serviceId;
    private final String tag;
    private final String method;

    private GetOperationsQuery(
            final ServiceId serviceId,
            final String tag,
            final String method) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId cannot be null");
        this.tag = tag;
        this.method = method;
    }

    /**
     * Creates a query to get all operations from a service.
     *
     * @param serviceId the service identifier
     * @return a new GetOperationsQuery
     */
    public static GetOperationsQuery forService(final ServiceId serviceId) {
        return new GetOperationsQuery(serviceId, null, null);
    }

    /**
     * Creates a query to get all operations from a service.
     *
     * @param serviceIdString the service identifier as string
     * @return a new GetOperationsQuery
     */
    public static GetOperationsQuery forService(final String serviceIdString) {
        return new GetOperationsQuery(ServiceId.fromString(serviceIdString), null, null);
    }

    /**
     * Creates a query to get operations filtered by tag.
     *
     * @param serviceId the service identifier
     * @param tag the tag to filter by
     * @return a new GetOperationsQuery
     */
    public static GetOperationsQuery byTag(final ServiceId serviceId, final String tag) {
        return new GetOperationsQuery(serviceId, tag, null);
    }

    /**
     * Creates a query to get operations filtered by HTTP method.
     *
     * @param serviceId the service identifier
     * @param method the HTTP method to filter by
     * @return a new GetOperationsQuery
     */
    public static GetOperationsQuery byMethod(final ServiceId serviceId, final String method) {
        return new GetOperationsQuery(serviceId, null, method);
    }

    /**
     * Returns the service identifier.
     *
     * @return the service ID
     */
    public ServiceId serviceId() {
        return serviceId;
    }

    /**
     * Returns the tag filter.
     *
     * @return the tag, or null for all
     */
    public String tag() {
        return tag;
    }

    /**
     * Returns the method filter.
     *
     * @return the method, or null for all
     */
    public String method() {
        return method;
    }

    /**
     * Checks if this query has a tag filter.
     *
     * @return true if filtered by tag
     */
    public boolean hasTagFilter() {
        return tag != null && !tag.isBlank();
    }

    /**
     * Checks if this query has a method filter.
     *
     * @return true if filtered by method
     */
    public boolean hasMethodFilter() {
        return method != null && !method.isBlank();
    }

    @Override
    public String toString() {
        return "GetOperationsQuery{"
            + "serviceId=" + serviceId
            + ", tag='" + tag + '\''
            + ", method='" + method + '\''
            + '}';
    }
}
