package co.fanki.openapimcp.infrastructure.mcp.tool;

import co.fanki.openapimcp.application.command.InvokeEndpointCommand;
import co.fanki.openapimcp.application.service.EndpointInvocationApplicationService;
import co.fanki.openapimcp.domain.model.InvocationResult;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpTool;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolHandler;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Tool for invoking microservice endpoints.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Component
public class InvokeEndpointTool implements McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeEndpointTool.class);

    private final EndpointInvocationApplicationService invocationService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new InvokeEndpointTool.
     *
     * @param invocationService the invocation service
     * @param objectMapper the JSON mapper
     */
    public InvokeEndpointTool(
            final EndpointInvocationApplicationService invocationService,
            final ObjectMapper objectMapper) {
        this.invocationService = Objects.requireNonNull(invocationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public McpTool definition() {
        return new McpTool(
            "invoke_endpoint",
            "Invokes a microservice endpoint. WARNING: This executes a real HTTP call to the service.",
            new McpTool.McpInputSchema(
                "object",
                Map.of(
                    "service_id", Map.of(
                        "type", "string",
                        "description", "Service ID in format namespace/name"
                    ),
                    "operation_id", Map.of(
                        "type", "string",
                        "description", "The operationId to invoke"
                    ),
                    "path_params", Map.of(
                        "type", "object",
                        "description", "Path parameters as key-value pairs"
                    ),
                    "query_params", Map.of(
                        "type", "object",
                        "description", "Query parameters as key-value pairs"
                    ),
                    "body", Map.of(
                        "type", "string",
                        "description", "Request body as JSON string"
                    )
                ),
                List.of("service_id", "operation_id")
            )
        );
    }

    @Override
    public McpToolResult execute(final Map<String, Object> arguments) {
        LOG.info("Executing invoke_endpoint tool with args: {}", arguments);

        try {
            final String serviceIdStr = (String) arguments.get("service_id");
            final String operationId = (String) arguments.get("operation_id");

            if (serviceIdStr == null || serviceIdStr.isBlank()) {
                return McpToolResult.error("service_id is required");
            }
            if (operationId == null || operationId.isBlank()) {
                return McpToolResult.error("operation_id is required");
            }

            final Map<String, String> pathParams = extractStringMap(
                arguments.get("path_params")
            );
            final Map<String, String> queryParams = extractStringMap(
                arguments.get("query_params")
            );
            final String body = extractBody(arguments.get("body"));

            final InvokeEndpointCommand command = InvokeEndpointCommand.builder()
                .serviceId(serviceIdStr)
                .operationId(operationId)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .body(body)
                .build();

            final InvocationResult result = invocationService.invoke(command);

            final Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("statusCode", result.statusCode());
            response.put("durationMs", result.durationMs());

            if (result.isFailure()) {
                response.put("error", result.errorMessage());
            } else {
                response.put("body", result.body());
                response.put("headers", result.headers());
            }

            final String json = objectMapper.writeValueAsString(response);
            return new McpToolResult(
                List.of(new McpToolResult.McpContent("text", json)),
                result.isFailure()
            );

        } catch (final EndpointInvocationApplicationService.InvocationException e) {
            return McpToolResult.error("Invocation failed: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            return McpToolResult.error("Invalid argument: " + e.getMessage());
        } catch (final Exception e) {
            LOG.error("Failed to invoke endpoint", e);
            return McpToolResult.error("Error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractStringMap(final Object obj) {
        if (obj == null) {
            return Map.of();
        }

        if (obj instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) obj;
            final Map<String, String> result = new HashMap<>();
            map.forEach((k, v) -> {
                if (v != null) {
                    result.put(k, String.valueOf(v));
                }
            });
            return result;
        }

        if (obj instanceof String) {
            try {
                return objectMapper.readValue(
                    (String) obj,
                    new TypeReference<Map<String, String>>() { }
                );
            } catch (final Exception e) {
                LOG.warn("Failed to parse params string: {}", obj);
                return Map.of();
            }
        }

        return Map.of();
    }

    private String extractBody(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (final Exception e) {
            LOG.warn("Failed to serialize body: {}", obj);
            return null;
        }
    }
}
