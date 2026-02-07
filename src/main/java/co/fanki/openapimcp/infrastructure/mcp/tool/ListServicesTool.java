package co.fanki.openapimcp.infrastructure.mcp.tool;

import co.fanki.openapimcp.application.query.ListServicesQuery;
import co.fanki.openapimcp.application.service.ServiceDiscoveryApplicationService;
import co.fanki.openapimcp.domain.model.DiscoveredService;
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

/**
 * MCP Tool for listing discovered microservices.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class ListServicesTool implements McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ListServicesTool.class);

    private final ServiceDiscoveryApplicationService discoveryService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new ListServicesTool.
     *
     * @param discoveryService the discovery service
     * @param objectMapper the JSON mapper
     */
    public ListServicesTool(
            final ServiceDiscoveryApplicationService discoveryService,
            final ObjectMapper objectMapper) {
        this.discoveryService = Objects.requireNonNull(discoveryService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public McpTool definition() {
        return new McpTool(
            "list_services",
            "Lists all microservices with OpenAPI specs available in the K8s cluster",
            new McpTool.McpInputSchema(
                "object",
                Map.of(
                    "namespace", Map.of(
                        "type", "string",
                        "description", "Filter by Kubernetes namespace (optional)"
                    ),
                    "active_only", Map.of(
                        "type", "boolean",
                        "description", "Only return active services with valid OpenAPI specs"
                    )
                ),
                List.of()
            )
        );
    }

    @Override
    public McpToolResult execute(final Map<String, Object> arguments) {
        LOG.info("Executing list_services tool with args: {}", arguments);

        try {
            final String namespace = (String) arguments.get("namespace");
            final Boolean activeOnly = (Boolean) arguments.getOrDefault("active_only", false);

            final ListServicesQuery query;
            if (Boolean.TRUE.equals(activeOnly)) {
                query = ListServicesQuery.activeOnly();
            } else if (namespace != null && !namespace.isBlank()) {
                query = ListServicesQuery.byNamespace(namespace);
            } else {
                query = ListServicesQuery.all();
            }

            final List<DiscoveredService> services = discoveryService.listServices(query);

            final List<Map<String, Object>> serviceList = services.stream()
                .map(this::serviceToMap)
                .toList();

            final Map<String, Object> result = Map.of(
                "services", serviceList,
                "total", services.size(),
                "suggestion", SourceCodeSuggestionPrompt.forServiceList(services)
            );

            final String json = objectMapper.writeValueAsString(result);
            return McpToolResult.success(json);

        } catch (final Exception e) {
            LOG.error("Failed to list services", e);
            return McpToolResult.error("Error: " + e.getMessage());
        }
    }

    private Map<String, Object> serviceToMap(final DiscoveredService service) {
        final int operationCount = service.specification()
            .map(spec -> spec.operationCount())
            .orElse(0);

        return Map.of(
            "id", service.id().asString(),
            "namespace", service.id().namespace(),
            "name", service.id().name(),
            "status", service.status().name(),
            "hasOpenApi", service.hasSpecification(),
            "operationCount", operationCount,
            "address", service.address().baseUrl()
        );
    }
}
