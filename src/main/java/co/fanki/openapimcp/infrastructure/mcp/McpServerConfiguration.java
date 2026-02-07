package co.fanki.openapimcp.infrastructure.mcp;

import co.fanki.openapimcp.infrastructure.mcp.protocol.McpMessage;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpTool;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolHandler;
import co.fanki.openapimcp.infrastructure.mcp.protocol.McpToolResult;
import co.fanki.openapimcp.infrastructure.mcp.tool.GetOperationDetailsTool;
import co.fanki.openapimcp.infrastructure.mcp.tool.GetOperationsTool;
import co.fanki.openapimcp.infrastructure.mcp.tool.InvokeEndpointTool;
import co.fanki.openapimcp.infrastructure.mcp.tool.ListServicesTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for the MCP Server.
 *
 * <p>Implements a JSON-RPC based MCP server using Spring WebFlux.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Configuration
public class McpServerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(McpServerConfiguration.class);

    @Value("${mcp.server.name:openapi-k8s-discovery}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    /**
     * Creates the MCP server endpoint router.
     *
     * @param listServicesTool the list services tool
     * @param getOperationsTool the get operations tool
     * @param getOperationDetailsTool the get operation details tool
     * @param invokeEndpointTool the invoke endpoint tool
     * @param objectMapper the JSON mapper
     * @return the router function
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRouter(
            final ListServicesTool listServicesTool,
            final GetOperationsTool getOperationsTool,
            final GetOperationDetailsTool getOperationDetailsTool,
            final InvokeEndpointTool invokeEndpointTool,
            final ObjectMapper objectMapper) {

        final Map<String, McpToolHandler> tools = new HashMap<>();
        tools.put("list_services", listServicesTool);
        tools.put("get_operations", getOperationsTool);
        tools.put("get_operation_details", getOperationDetailsTool);
        tools.put("invoke_endpoint", invokeEndpointTool);

        LOG.info("Initializing MCP Server: {} v{} with {} tools",
            serverName, serverVersion, tools.size());

        return RouterFunctions.route()
            .POST("/mcp", request -> handleMcpRequest(request, tools, objectMapper))
            .GET("/mcp/info", request -> handleInfo())
            .build();
    }

    private Mono<ServerResponse> handleMcpRequest(
            final ServerRequest request,
            final Map<String, McpToolHandler> tools,
            final ObjectMapper objectMapper) {

        return request.bodyToMono(McpMessage.class)
            .flatMap(message -> {
                try {
                    final McpMessage response = processMessage(message, tools, objectMapper);
                    return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response);
                } catch (final Exception e) {
                    LOG.error("Error processing MCP message", e);
                    return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(McpMessage.errorResponse(
                            message.id(),
                            -32603,
                            e.getMessage()
                        ));
                }
            });
    }

    @SuppressWarnings("unchecked")
    private McpMessage processMessage(
            final McpMessage message,
            final Map<String, McpToolHandler> tools,
            final ObjectMapper objectMapper) {

        LOG.debug("Processing MCP method: {}", message.method());

        return switch (message.method()) {
            case "initialize" -> handleInitialize(message);
            case "tools/list" -> handleListTools(message, tools);
            case "tools/call" -> handleCallTool(message, tools, objectMapper);
            default -> McpMessage.errorResponse(
                message.id(),
                -32601,
                "Method not found: " + message.method()
            );
        };
    }

    private McpMessage handleInitialize(final McpMessage message) {
        final Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of()
            ),
            "serverInfo", Map.of(
                "name", serverName,
                "version", serverVersion
            )
        );
        return McpMessage.response(message.id(), result);
    }

    private McpMessage handleListTools(
            final McpMessage message,
            final Map<String, McpToolHandler> tools) {

        final List<Map<String, Object>> toolList = tools.values().stream()
            .map(handler -> {
                final McpTool def = handler.definition();
                return Map.<String, Object>of(
                    "name", def.name(),
                    "description", def.description(),
                    "inputSchema", Map.of(
                        "type", def.inputSchema().type(),
                        "properties", def.inputSchema().properties(),
                        "required", def.inputSchema().required()
                    )
                );
            })
            .collect(Collectors.toList());

        return McpMessage.response(message.id(), Map.of("tools", toolList));
    }

    @SuppressWarnings("unchecked")
    private McpMessage handleCallTool(
            final McpMessage message,
            final Map<String, McpToolHandler> tools,
            final ObjectMapper objectMapper) {

        final Map<String, Object> params = message.params();
        if (params == null) {
            return McpMessage.errorResponse(message.id(), -32602, "Missing params");
        }

        final String toolName = (String) params.get("name");
        if (toolName == null) {
            return McpMessage.errorResponse(message.id(), -32602, "Missing tool name");
        }

        final McpToolHandler handler = tools.get(toolName);
        if (handler == null) {
            return McpMessage.errorResponse(message.id(), -32602, "Unknown tool: " + toolName);
        }

        final Map<String, Object> arguments = (Map<String, Object>) params
            .getOrDefault("arguments", Map.of());

        LOG.info("Executing tool: {} with args: {}", toolName, arguments);

        final McpToolResult result = handler.execute(arguments);

        final List<Map<String, Object>> content = result.content().stream()
            .map(c -> Map.<String, Object>of("type", c.type(), "text", c.text()))
            .collect(Collectors.toList());

        return McpMessage.response(message.id(), Map.of(
            "content", content,
            "isError", result.isError()
        ));
    }

    private Mono<ServerResponse> handleInfo() {
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "name", serverName,
                "version", serverVersion,
                "protocol", "MCP",
                "protocolVersion", "2024-11-05"
            ));
    }
}
