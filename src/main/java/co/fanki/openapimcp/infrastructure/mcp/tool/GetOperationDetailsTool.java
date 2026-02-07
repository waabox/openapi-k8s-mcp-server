package co.fanki.openapimcp.infrastructure.mcp.tool;

import co.fanki.openapimcp.application.query.GetOperationDetailsQuery;
import co.fanki.openapimcp.application.service.ServiceDiscoveryApplicationService;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.OperationParameter;
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
 * MCP Tool for getting detailed information about a specific operation.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class GetOperationDetailsTool implements McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GetOperationDetailsTool.class);

    private final ServiceDiscoveryApplicationService discoveryService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new GetOperationDetailsTool.
     *
     * @param discoveryService the discovery service
     * @param objectMapper the JSON mapper
     */
    public GetOperationDetailsTool(
            final ServiceDiscoveryApplicationService discoveryService,
            final ObjectMapper objectMapper) {
        this.discoveryService = Objects.requireNonNull(discoveryService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public McpTool definition() {
        return new McpTool(
            "get_operation_details",
            "Gets complete details of an operation: parameters, request body schema, response schemas",
            new McpTool.McpInputSchema(
                "object",
                Map.of(
                    "service_id", Map.of(
                        "type", "string",
                        "description", "Service ID in format namespace/name"
                    ),
                    "operation_id", Map.of(
                        "type", "string",
                        "description", "The operationId from OpenAPI"
                    )
                ),
                List.of("service_id", "operation_id")
            )
        );
    }

    @Override
    public McpToolResult execute(final Map<String, Object> arguments) {
        LOG.info("Executing get_operation_details tool with args: {}", arguments);

        try {
            final String serviceIdStr = (String) arguments.get("service_id");
            final String operationId = (String) arguments.get("operation_id");

            if (serviceIdStr == null || serviceIdStr.isBlank()) {
                return McpToolResult.error("service_id is required");
            }
            if (operationId == null || operationId.isBlank()) {
                return McpToolResult.error("operation_id is required");
            }

            final GetOperationDetailsQuery query = GetOperationDetailsQuery.of(
                serviceIdStr, operationId
            );

            final Optional<ServiceDiscoveryApplicationService.OperationDetails> detailsOpt =
                discoveryService.getOperationDetails(query);

            if (detailsOpt.isEmpty()) {
                return McpToolResult.error(
                    "Operation not found: " + operationId + " in service " + serviceIdStr
                );
            }

            final var details = detailsOpt.get();
            final Operation op = details.operation();

            final List<Map<String, Object>> parameters = op.parameters().stream()
                .map(this::parameterToMap)
                .toList();

            final Map<String, Object> result = new java.util.HashMap<>();
            result.put("service_id", serviceIdStr);
            result.put("operation_id", op.operationId());
            result.put("method", op.method());
            result.put("path", op.path());
            result.put("endpoint_url", details.endpointUrl());
            result.put("summary", op.summary() != null ? op.summary() : "");
            result.put("description", op.description() != null ? op.description() : "");
            result.put("tag", op.tag() != null ? op.tag() : "");
            result.put("parameters", parameters);
            result.put("requestBodySchema", op.requestBodySchema() != null ? op.requestBodySchema() : "");
            result.put("responseSchema", op.responseSchema() != null ? op.responseSchema() : "");

            final String json = objectMapper.writeValueAsString(result);
            return McpToolResult.success(json);

        } catch (final IllegalArgumentException e) {
            return McpToolResult.error("Invalid argument: " + e.getMessage());
        } catch (final Exception e) {
            LOG.error("Failed to get operation details", e);
            return McpToolResult.error("Error: " + e.getMessage());
        }
    }

    private Map<String, Object> parameterToMap(final OperationParameter param) {
        return Map.of(
            "name", param.name(),
            "in", param.in().name().toLowerCase(),
            "required", param.required(),
            "description", param.description() != null ? param.description() : "",
            "schema", param.schema() != null ? param.schema() : ""
        );
    }
}
