package co.fanki.openapimcp.infrastructure.mcp.protocol;

import java.util.List;
import java.util.Map;

/**
 * Represents an MCP tool definition and its input schema.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public record McpTool(
    String name,
    String description,
    McpInputSchema inputSchema
) {
    /**
     * Input schema for an MCP tool.
     */
    public record McpInputSchema(
        String type,
        Map<String, Map<String, Object>> properties,
        List<String> required
    ) {
    }
}
