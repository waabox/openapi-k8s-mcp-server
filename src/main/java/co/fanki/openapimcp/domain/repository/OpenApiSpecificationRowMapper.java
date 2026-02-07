package co.fanki.openapimcp.domain.repository;

import co.fanki.openapimcp.domain.model.OpenApiSpecification;
import co.fanki.openapimcp.domain.model.Operation;
import co.fanki.openapimcp.domain.model.OperationParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JDBI RowMapper for {@link OpenApiSpecification}.
 *
 * <p>Maps database rows to domain objects, deserializing the operations JSON.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public class OpenApiSpecificationRowMapper implements RowMapper<OpenApiSpecification> {

    private static final Logger LOG = LoggerFactory.getLogger(
        OpenApiSpecificationRowMapper.class
    );

    private final ObjectMapper objectMapper;

    /**
     * Creates a new OpenApiSpecificationRowMapper.
     *
     * @param objectMapper the JSON object mapper
     */
    public OpenApiSpecificationRowMapper(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public OpenApiSpecification map(final ResultSet rs, final StatementContext ctx)
            throws SQLException {

        final String title = rs.getString("title");
        final String version = rs.getString("version");
        final String rawJson = rs.getString("raw_json");
        final String operationsJson = rs.getString("operations_json");
        final LocalDateTime fetchedAt = toLocalDateTime(rs.getTimestamp("fetched_at"));

        final List<Operation> operations = deserializeOperations(operationsJson);

        return OpenApiSpecification.reconstitute(
            title,
            version,
            rawJson,
            operations,
            fetchedAt
        );
    }

    private LocalDateTime toLocalDateTime(final Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private List<Operation> deserializeOperations(final String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return List.of();
        }

        try {
            final List<Map<String, Object>> opMaps = objectMapper.readValue(
                json,
                new TypeReference<List<Map<String, Object>>>() { }
            );

            final List<Operation> operations = new ArrayList<>();
            for (final Map<String, Object> map : opMaps) {
                operations.add(mapToOperation(map));
            }
            return operations;
        } catch (final JsonProcessingException e) {
            LOG.error("Failed to deserialize operations", e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Operation mapToOperation(final Map<String, Object> map) {
        final List<Map<String, Object>> paramMaps =
            (List<Map<String, Object>>) map.getOrDefault("parameters", List.of());

        final List<OperationParameter> params = paramMaps.stream()
            .map(this::mapToParameter)
            .toList();

        return Operation.builder()
            .operationId((String) map.get("operationId"))
            .method((String) map.get("method"))
            .path((String) map.get("path"))
            .summary(nullIfEmpty((String) map.get("summary")))
            .description(nullIfEmpty((String) map.get("description")))
            .tag(nullIfEmpty((String) map.get("tag")))
            .parameters(params)
            .requestBodySchema(nullIfEmpty((String) map.get("requestBodySchema")))
            .responseSchema(nullIfEmpty((String) map.get("responseSchema")))
            .build();
    }

    private OperationParameter mapToParameter(final Map<String, Object> map) {
        return OperationParameter.of(
            (String) map.get("name"),
            OperationParameter.In.valueOf((String) map.get("in")),
            (Boolean) map.getOrDefault("required", false),
            nullIfEmpty((String) map.get("description")),
            nullIfEmpty((String) map.get("schema"))
        );
    }

    private String nullIfEmpty(final String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
