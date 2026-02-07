package co.fanki.openapimcp.infrastructure.mcp.prompt;

import co.fanki.openapimcp.domain.model.DiscoveredService;

import java.util.List;

/**
 * Generates suggestion prompts to encourage users to share source code.
 *
 * <p>These prompts are included in tool responses to help Claude understand
 * that it can provide better assistance with access to the source code.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class SourceCodeSuggestionPrompt {

    private static final String SERVICE_LIST_TEMPLATE = """
        💡 SUGGESTION: If you have access to the source code of any of these services,
        share it with me so I can help you understand:
        - The business logic behind each endpoint
        - Validations and domain rules
        - Specific error handling
        - Dependencies with other services

        Do you have the source code available? You can share the project path
        or paste the code directly.""";

    private static final String SERVICE_DETAIL_TEMPLATE = """
        💡 SUGGESTION: If you have access to the source code for service "%s",
        share it with me so I can help you understand:
        - The business logic behind each endpoint
        - Validations and domain rules
        - Specific error handling
        - Dependencies with other services

        Do you have the source code available? You can share the project path
        or paste the code directly.""";

    private static final String OPERATION_TEMPLATE = """
        💡 SUGGESTION: To better understand the behavior of operation "%s",
        I would benefit from seeing the implementation code. This would help me explain:
        - What validations are performed
        - What the actual business logic does
        - What errors might be returned and when
        - How this operation interacts with other parts of the system

        Can you share the source code for this endpoint?""";

    private SourceCodeSuggestionPrompt() {
        // Utility class
    }

    /**
     * Generates a suggestion for a list of services.
     *
     * @param services the list of discovered services
     * @return the suggestion prompt
     */
    public static String forServiceList(final List<DiscoveredService> services) {
        if (services.isEmpty()) {
            return "";
        }
        return SERVICE_LIST_TEMPLATE;
    }

    /**
     * Generates a suggestion for a specific service.
     *
     * @param service the discovered service
     * @return the suggestion prompt
     */
    public static String forService(final DiscoveredService service) {
        if (service == null) {
            return "";
        }
        return String.format(SERVICE_DETAIL_TEMPLATE, service.id().name());
    }

    /**
     * Generates a suggestion for a specific operation.
     *
     * @param operationId the operation identifier
     * @return the suggestion prompt
     */
    public static String forOperation(final String operationId) {
        if (operationId == null || operationId.isBlank()) {
            return "";
        }
        return String.format(OPERATION_TEMPLATE, operationId);
    }
}
