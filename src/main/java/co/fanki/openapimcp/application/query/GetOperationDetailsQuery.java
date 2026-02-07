package co.fanki.openapimcp.application.query;

import co.fanki.openapimcp.domain.model.ServiceId;

import java.util.Objects;

/**
 * Query to get detailed information about a specific operation.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class GetOperationDetailsQuery {

    private final ServiceId serviceId;
    private final String operationId;

    private GetOperationDetailsQuery(
            final ServiceId serviceId,
            final String operationId) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId cannot be null");
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");
    }

    /**
     * Creates a query to get operation details.
     *
     * @param serviceId the service identifier
     * @param operationId the operation identifier
     * @return a new GetOperationDetailsQuery
     */
    public static GetOperationDetailsQuery of(
            final ServiceId serviceId,
            final String operationId) {
        return new GetOperationDetailsQuery(serviceId, operationId);
    }

    /**
     * Creates a query to get operation details.
     *
     * @param serviceIdString the service identifier as string
     * @param operationId the operation identifier
     * @return a new GetOperationDetailsQuery
     */
    public static GetOperationDetailsQuery of(
            final String serviceIdString,
            final String operationId) {
        return new GetOperationDetailsQuery(
            ServiceId.fromString(serviceIdString),
            operationId
        );
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
     * Returns the operation identifier.
     *
     * @return the operation ID
     */
    public String operationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "GetOperationDetailsQuery{"
            + "serviceId=" + serviceId
            + ", operationId='" + operationId + '\''
            + '}';
    }
}
