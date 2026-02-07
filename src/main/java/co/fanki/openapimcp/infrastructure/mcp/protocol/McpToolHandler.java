package co.fanki.openapimcp.infrastructure.mcp.protocol;

import java.util.Map;

/**
 * Interface for MCP tool handlers.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public interface McpToolHandler {

    /**
     * Returns the tool definition.
     *
     * @return the MCP tool
     */
    McpTool definition();

    /**
     * Executes the tool with the given arguments.
     *
     * @param arguments the tool arguments
     * @return the tool result
     */
    McpToolResult execute(Map<String, Object> arguments);
}
