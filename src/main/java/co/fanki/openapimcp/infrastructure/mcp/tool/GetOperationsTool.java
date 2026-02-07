package co.fanki.openapimcp.infrastructure.mcp.tool;

import co.fanki.openapimcp.application.query.GetOperationsQuery;
import co.fanki.openapimcp.application.service.ServiceDiscoveryApplicationService;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.ServiceId;
import co.fanki.openapimcp.infrastructure.mcp.prompt.SourceCodeSuggestionPrompt;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpTool;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolHandler;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MCP Tool for getting operations from a service.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class GetOperationsTool implements McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GetOperationsTool.class);

    private final ServiceDiscoveryApplicationService discoveryService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new GetOperationsTool.
     *
     * @param discoveryService the discovery service
     * @param objectMapper the JSON mapper
     */
    public GetOperationsTool(
            final ServiceDiscoveryApplicationService discoveryService,
            final ObjectMapper objectMapper) {
        this.discoveryService = Objects.requireNonNull(discoveryService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public McpTool definition() {
        return new McpTool(
            "get_operations",
            "Lists all HTTP operations (endpoints) available in a microservice",
            new McpTool.McpInputSchema(
                "object",
                Map.of(
                    "service_id", Map.of(
                        "type", "string",
                        "description", "Service ID in format namespace/name"
                    ),
                    "tag", Map.of(
                        "type", "string",
                        "description", "Filter by OpenAPI tag (optional)"
                    ),
                    "method", Map.of(
                        "type", "string",
                        "description", "Filter by HTTP method: GET, POST, PUT, DELETE (optional)"
                    )
                ),
                List.of("service_id")
            )
        );
    }

    @Override
    public McpToolResult execute(final Map<String, Object> arguments) {
        LOG.info("Executing get_operations tool with args: {}", arguments);

        try {
            final String serviceIdStr = (String) arguments.get("service_id");
            if (serviceIdStr == null || serviceIdStr.isBlank()) {
                return McpToolResult.error("service_id is required");
            }

            final String tag = (String) arguments.get("tag");
            final String method = (String) arguments.get("method");

            final ServiceId serviceId = ServiceId.fromString(serviceIdStr);
            final Optional<DiscoveredService> serviceOpt = discoveryService.findService(serviceIdStr);

            if (serviceOpt.isEmpty()) {
                return McpToolResult.error("Service not found: " + serviceIdStr);
            }

            final DiscoveredService service = serviceOpt.get();

            if (!service.hasSpecification()) {
                return McpToolResult.error("Service has no OpenAPI specification: " + serviceIdStr);
            }

            final GetOperationsQuery query;
            if (tag != null && !tag.isBlank()) {
                query = GetOperationsQuery.byTag(serviceId, tag);
            } else if (method != null && !method.isBlank()) {
                query = GetOperationsQuery.byMethod(serviceId, method);
            } else {
                query = GetOperationsQuery.forService(serviceId);
            }

            final List<Operation> operations = discoveryService.getOperations(query);

            final List<Map<String, Object>> operationList = operations.stream()
                .map(this::operationToMap)
                .toList();

            final Map<String, Object> result = Map.of(
                "service_id", serviceIdStr,
                "operations", operationList,
                "total", operations.size(),
                "tags", service.specification().map(s -> s.tags()).orElse(List.of()),
                "suggestion", SourceCodeSuggestionPrompt.forService(service)
            );

            final String json = objectMapper.writeValueAsString(result);
            return McpToolResult.success(json);

        } catch (final IllegalArgumentException e) {
            return McpToolResult.error("Invalid argument: " + e.getMessage());
        } catch (final Exception e) {
            LOG.error("Failed to get operations", e);
            return McpToolResult.error("Error: " + e.getMessage());
        }
    }

    private Map<String, Object> operationToMap(final Operation op) {
        return Map.of(
            "operationId", op.operationId(),
            "method", op.method(),
            "path", op.path(),
            "summary", op.summary() != null ? op.summary() : "",
            "tag", op.tag() != null ? op.tag() : "",
            "hasRequestBody", op.hasRequestBody(),
            "parameterCount", op.parameters().size()
        );
    }
}
