package co.fanki.openapimcp.infrastructure.mcp.protocol;

import java.util.List;

/**
 * Represents the result of executing an MCP tool.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public record McpToolResult(
    List<McpContent> content,
    boolean isError
) {

    /**
     * Creates a successful result with text content.
     *
     * @param text the text content
     * @return a new McpToolResult
     */
    public static McpToolResult success(final String text) {
        return new McpToolResult(
            List.of(new McpContent("text", text)),
            false
        );
    }

    /**
     * Creates an error result with a message.
     *
     * @param errorMessage the error message
     * @return a new McpToolResult
     */
    public static McpToolResult error(final String errorMessage) {
        return new McpToolResult(
            List.of(new McpContent("text", errorMessage)),
            true
        );
    }

    /**
     * Represents content in an MCP response.
     */
    public record McpContent(String type, String text) {
    }
}
