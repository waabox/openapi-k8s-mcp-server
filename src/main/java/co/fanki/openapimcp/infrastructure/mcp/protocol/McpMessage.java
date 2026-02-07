package co.fanki.openapimcp.infrastructure.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Represents a JSON-RPC message in the MCP protocol.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpMessage(
    String jsonrpc,
    Object id,
    String method,
    Map<String, Object> params,
    Object result,
    McpError error
) {

    /**
     * Creates a new request message.
     *
     * @param id the request ID
     * @param method the method name
     * @param params the parameters
     * @return a new McpMessage
     */
    public static McpMessage request(final Object id, final String method,
            final Map<String, Object> params) {
        return new McpMessage("2.0", id, method, params, null, null);
    }

    /**
     * Creates a new response message.
     *
     * @param id the request ID
     * @param result the result
     * @return a new McpMessage
     */
    public static McpMessage response(final Object id, final Object result) {
        return new McpMessage("2.0", id, null, null, result, null);
    }

    /**
     * Creates a new error response message.
     *
     * @param id the request ID
     * @param code the error code
     * @param message the error message
     * @return a new McpMessage
     */
    public static McpMessage errorResponse(final Object id, final int code,
            final String message) {
        return new McpMessage("2.0", id, null, null, null, new McpError(code, message));
    }

    /**
     * Represents an error in JSON-RPC.
     */
    public record McpError(int code, String message) {
    }
}
