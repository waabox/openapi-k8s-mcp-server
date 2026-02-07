package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.Operation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain service for finding and matching operations across services.
 *
 * <p>Provides methods to search for operations by various criteria,
 * enabling efficient lookup for the MCP tools.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class OperationMatcher {

    /**
     * Finds an operation by ID within a service.
     *
     * @param service the service to search
     * @param operationId the operation ID
     * @return the operation, or empty if not found
     */
    public Optional<Operation> findByOperationId(
            final DiscoveredService service,
            final String operationId) {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(operationId, "operationId cannot be null");

        return service.findOperation(operationId);
    }

    /**
     * Finds operations matching a path pattern.
     *
     * @param service the service to search
     * @param pathPattern the path pattern (supports wildcards *)
     * @return list of matching operations
     */
    public List<Operation> findByPath(
            final DiscoveredService service,
            final String pathPattern) {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(pathPattern, "pathPattern cannot be null");

        final String regex = pathPattern
            .replace("*", ".*")
            .replace("{", "\\{")
            .replace("}", "\\}");

        return service.allOperations().stream()
            .filter(op -> op.path().matches(regex))
            .toList();
    }

    /**
     * Finds operations by HTTP method.
     *
     * @param service the service to search
     * @param method the HTTP method (GET, POST, etc.)
     * @return list of matching operations
     */
    public List<Operation> findByMethod(
            final DiscoveredService service,
            final String method) {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(method, "method cannot be null");

        final String upperMethod = method.toUpperCase();
        return service.allOperations().stream()
            .filter(op -> upperMethod.equals(op.method()))
            .toList();
    }

    /**
     * Finds operations by tag.
     *
     * @param service the service to search
     * @param tag the OpenAPI tag
     * @return list of matching operations
     */
    public List<Operation> findByTag(
            final DiscoveredService service,
            final String tag) {
        Objects.requireNonNull(service, "service cannot be null");

        return service.operations(tag);
    }

    /**
     * Finds operations matching a keyword in summary or description.
     *
     * @param service the service to search
     * @param keyword the keyword to search for
     * @return list of matching operations
     */
    public List<Operation> findByKeyword(
            final DiscoveredService service,
            final String keyword) {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(keyword, "keyword cannot be null");

        final String lowerKeyword = keyword.toLowerCase();
        return service.allOperations().stream()
            .filter(op -> matchesKeyword(op, lowerKeyword))
            .toList();
    }

    private boolean matchesKeyword(final Operation op, final String keyword) {
        if (containsIgnoreCase(op.operationId(), keyword)) {
            return true;
        }
        if (containsIgnoreCase(op.summary(), keyword)) {
            return true;
        }
        if (containsIgnoreCase(op.description(), keyword)) {
            return true;
        }
        if (containsIgnoreCase(op.path(), keyword)) {
            return true;
        }
        return false;
    }

    private boolean containsIgnoreCase(final String text, final String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    /**
     * Finds operations across multiple services matching the criteria.
     *
     * @param services the services to search
     * @param tag the tag filter (optional)
     * @param method the method filter (optional)
     * @return list of matching operations with their service
     */
    public List<OperationMatch> findAcrossServices(
            final List<DiscoveredService> services,
            final String tag,
            final String method) {
        Objects.requireNonNull(services, "services cannot be null");

        return services.stream()
            .filter(DiscoveredService::hasSpecification)
            .flatMap(service -> {
                List<Operation> ops = service.allOperations();

                if (tag != null && !tag.isBlank()) {
                    ops = ops.stream()
                        .filter(op -> tag.equals(op.tag()))
                        .toList();
                }

                if (method != null && !method.isBlank()) {
                    final String upperMethod = method.toUpperCase();
                    ops = ops.stream()
                        .filter(op -> upperMethod.equals(op.method()))
                        .toList();
                }

                return ops.stream()
                    .map(op -> new OperationMatch(service, op));
            })
            .toList();
    }

    /**
     * Represents a matched operation with its owning service.
     *
     * @param service the owning service
     * @param operation the matched operation
     */
    public record OperationMatch(DiscoveredService service, Operation operation) {

        public OperationMatch {
            Objects.requireNonNull(service);
            Objects.requireNonNull(operation);
        }
    }
}
