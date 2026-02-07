package co.fanki.openapimcp.application.service;

import co.fanki.openapimcp.application.command.InvokeEndpointCommand;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.InvocationResult;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.repository.DiscoveredServiceRepository;
import co.fanki.openapimcp.domain.service.EndpointInvoker;
import co.fanki.openapimcp.infrastructure.http.EndpointHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Application service for invoking microservice endpoints.
 *
 * <p>This service orchestrates the invocation of HTTP endpoints on
 * discovered microservices, handling validation, request building,
 * and result processing.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class EndpointInvocationApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(
        EndpointInvocationApplicationService.class
    );

    private final DiscoveredServiceRepository serviceRepository;
    private final EndpointInvoker endpointInvoker;
    private final EndpointHttpClient httpClient;

    /**
     * Creates a new EndpointInvocationApplicationService.
     *
     * @param serviceRepository the service repository
     * @param endpointInvoker the endpoint invoker
     * @param httpClient the HTTP client
     */
    public EndpointInvocationApplicationService(
            final DiscoveredServiceRepository serviceRepository,
            final EndpointInvoker endpointInvoker,
            final EndpointHttpClient httpClient) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository);
        this.endpointInvoker = Objects.requireNonNull(endpointInvoker);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    /**
     * Invokes an endpoint based on the command.
     *
     * @param command the invocation command
     * @return the invocation result
     * @throws InvocationException if invocation fails
     */
    public InvocationResult invoke(final InvokeEndpointCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        LOG.info("Invoking endpoint: service={}, operation={}",
            command.serviceId(), command.operationId());

        // Find the service
        final Optional<DiscoveredService> serviceOpt = serviceRepository
            .findById(command.serviceId());

        if (serviceOpt.isEmpty()) {
            throw new InvocationException("Service not found: " + command.serviceId());
        }

        final DiscoveredService service = serviceOpt.get();

        // Check if service is active
        if (!service.isActive()) {
            throw new InvocationException(
                "Service is not active: " + service.status().description()
            );
        }

        // Find the operation
        final Optional<Operation> operationOpt = service.findOperation(command.operationId());

        if (operationOpt.isEmpty()) {
            throw new InvocationException(
                "Operation not found: " + command.operationId() + " in service " + command.serviceId()
            );
        }

        final Operation operation = operationOpt.get();

        // Prepare the invocation
        final EndpointInvoker.InvocationRequest request;
        try {
            request = endpointInvoker.prepareInvocation(
                service,
                operation,
                command.pathParams(),
                command.queryParams(),
                command.body()
            );
        } catch (final EndpointInvoker.InvocationValidationException e) {
            throw new InvocationException("Validation failed: " + e.getMessage(), e);
        }

        LOG.debug("Prepared request: {} {}", request.method(), request.url());

        // Execute the HTTP call
        final InvocationResult result = httpClient.invoke(request);

        if (result.isFailure()) {
            LOG.warn("Invocation failed: {} {}, error: {}",
                request.method(), request.url(), result.errorMessage());
        } else {
            LOG.info("Invocation succeeded: {} {} -> {} in {}ms",
                request.method(), request.url(), result.statusCode(), result.durationMs());
        }

        return result;
    }

    /**
     * Tests if an endpoint is reachable without executing any side effects.
     *
     * <p>This is a dry-run that only tests connectivity.</p>
     *
     * @param command the invocation command
     * @return true if the endpoint is reachable
     */
    public boolean testConnection(final InvokeEndpointCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        try {
            final Optional<DiscoveredService> serviceOpt = serviceRepository
                .findById(command.serviceId());

            if (serviceOpt.isEmpty()) {
                return false;
            }

            final DiscoveredService service = serviceOpt.get();
            return service.isActive() && service.hasSpecification();

        } catch (final Exception e) {
            LOG.debug("Connection test failed for {}: {}", command.serviceId(), e.getMessage());
            return false;
        }
    }

    /**
     * Exception thrown when an invocation fails.
     */
    public static class InvocationException extends RuntimeException {

        /**
         * Creates a new InvocationException.
         *
         * @param message the error message
         */
        public InvocationException(final String message) {
            super(message);
        }

        /**
         * Creates a new InvocationException with a cause.
         *
         * @param message the error message
         * @param cause the underlying cause
         */
        public InvocationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
