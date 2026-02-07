package co.fanki.openapimcp.domain.service;

import co.fanki.openapimcp.domain.model.OpenApiSpecification;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.OperationParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain service responsible for parsing OpenAPI specifications.
 *
 * <p>Converts raw JSON OpenAPI documents into domain objects that can be
 * stored and queried efficiently.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Service
public class OpenApiParser {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new OpenApiParser.
     *
     * @param objectMapper the JSON object mapper
     */
    public OpenApiParser(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Parses a raw JSON OpenAPI specification into domain objects.
     *
     * @param rawJson the raw JSON OpenAPI specification
     * @return the parsed OpenApiSpecification
     * @throws OpenApiParseException if parsing fails
     */
    public OpenApiSpecification parse(final String rawJson) {
        Objects.requireNonNull(rawJson, "rawJson cannot be null");

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        final SwaggerParseResult result = new OpenAPIV3Parser().readContents(rawJson, null, options);

        if (result.getOpenAPI() == null) {
            final String errors = result.getMessages() != null
                ? String.join(", ", result.getMessages())
                : "Unknown parsing error";
            throw new OpenApiParseException("Failed to parse OpenAPI specification: " + errors);
        }

        final OpenAPI openApi = result.getOpenAPI();
        final List<Operation> operations = extractOperations(openApi);

        final String title = openApi.getInfo() != null ? openApi.getInfo().getTitle() : null;
        final String version = openApi.getInfo() != null ? openApi.getInfo().getVersion() : null;

        return OpenApiSpecification.of(title, version, rawJson, operations);
    }

    private List<Operation> extractOperations(final OpenAPI openApi) {
        final List<Operation> operations = new ArrayList<>();

        if (openApi.getPaths() == null) {
            return operations;
        }

        for (final Map.Entry<String, PathItem> pathEntry : openApi.getPaths().entrySet()) {
            final String path = pathEntry.getKey();
            final PathItem pathItem = pathEntry.getValue();

            extractOperationFromMethod(operations, path, "GET", pathItem.getGet());
            extractOperationFromMethod(operations, path, "POST", pathItem.getPost());
            extractOperationFromMethod(operations, path, "PUT", pathItem.getPut());
            extractOperationFromMethod(operations, path, "DELETE", pathItem.getDelete());
            extractOperationFromMethod(operations, path, "PATCH", pathItem.getPatch());
            extractOperationFromMethod(operations, path, "HEAD", pathItem.getHead());
            extractOperationFromMethod(operations, path, "OPTIONS", pathItem.getOptions());
        }

        return operations;
    }

    private void extractOperationFromMethod(
            final List<Operation> operations,
            final String path,
            final String method,
            final io.swagger.v3.oas.models.Operation swaggerOp) {

        if (swaggerOp == null) {
            return;
        }

        final String operationId = swaggerOp.getOperationId() != null
            ? swaggerOp.getOperationId()
            : generateOperationId(method, path);

        final String tag = swaggerOp.getTags() != null && !swaggerOp.getTags().isEmpty()
            ? swaggerOp.getTags().get(0)
            : null;

        final List<OperationParameter> parameters = extractParameters(swaggerOp);
        final String requestBodySchema = extractRequestBodySchema(swaggerOp);
        final String responseSchema = extractResponseSchema(swaggerOp);

        final Operation operation = Operation.builder()
            .operationId(operationId)
            .method(method)
            .path(path)
            .summary(swaggerOp.getSummary())
            .description(swaggerOp.getDescription())
            .tag(tag)
            .parameters(parameters)
            .requestBodySchema(requestBodySchema)
            .responseSchema(responseSchema)
            .build();

        operations.add(operation);
    }

    private String generateOperationId(final String method, final String path) {
        final String cleanPath = path.replaceAll("[{}]", "")
            .replaceAll("/", "_")
            .replaceAll("-", "_");
        return method.toLowerCase() + cleanPath;
    }

    private List<OperationParameter> extractParameters(
            final io.swagger.v3.oas.models.Operation swaggerOp) {

        if (swaggerOp.getParameters() == null) {
            return List.of();
        }

        final List<OperationParameter> params = new ArrayList<>();

        for (final Parameter param : swaggerOp.getParameters()) {
            final OperationParameter.In in = parseParameterIn(param.getIn());
            if (in == null) {
                continue;
            }

            final String schema = schemaToJson(param.getSchema());

            params.add(OperationParameter.of(
                param.getName(),
                in,
                Boolean.TRUE.equals(param.getRequired()),
                param.getDescription(),
                schema
            ));
        }

        return params;
    }

    private OperationParameter.In parseParameterIn(final String in) {
        if (in == null) {
            return null;
        }
        return switch (in.toLowerCase()) {
            case "path" -> OperationParameter.In.PATH;
            case "query" -> OperationParameter.In.QUERY;
            case "header" -> OperationParameter.In.HEADER;
            case "cookie" -> OperationParameter.In.COOKIE;
            default -> null;
        };
    }

    private String extractRequestBodySchema(final io.swagger.v3.oas.models.Operation swaggerOp) {
        if (swaggerOp.getRequestBody() == null
            || swaggerOp.getRequestBody().getContent() == null) {
            return null;
        }

        final var content = swaggerOp.getRequestBody().getContent();
        final var jsonContent = content.get("application/json");

        if (jsonContent != null && jsonContent.getSchema() != null) {
            return schemaToJson(jsonContent.getSchema());
        }

        return null;
    }

    private String extractResponseSchema(final io.swagger.v3.oas.models.Operation swaggerOp) {
        if (swaggerOp.getResponses() == null) {
            return null;
        }

        final var successResponse = swaggerOp.getResponses().get("200");
        if (successResponse == null) {
            final var createdResponse = swaggerOp.getResponses().get("201");
            if (createdResponse != null && createdResponse.getContent() != null) {
                return extractSchemaFromContent(createdResponse.getContent());
            }
            return null;
        }

        if (successResponse.getContent() == null) {
            return null;
        }

        return extractSchemaFromContent(successResponse.getContent());
    }

    private String extractSchemaFromContent(
            final io.swagger.v3.oas.models.media.Content content) {

        final var jsonContent = content.get("application/json");
        if (jsonContent != null && jsonContent.getSchema() != null) {
            return schemaToJson(jsonContent.getSchema());
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private String schemaToJson(final Schema schema) {
        if (schema == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (final JsonProcessingException e) {
            return "{\"type\": \"" + schema.getType() + "\"}";
        }
    }

    /**
     * Exception thrown when OpenAPI parsing fails.
     */
    public static class OpenApiParseException extends RuntimeException {

        /**
         * Creates a new OpenApiParseException.
         *
         * @param message the error message
         */
        public OpenApiParseException(final String message) {
            super(message);
        }

        /**
         * Creates a new OpenApiParseException with a cause.
         *
         * @param message the error message
         * @param cause the underlying cause
         */
        public OpenApiParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
