package co.fanki.openapimcp.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity representing the OpenAPI specification for a service.
 *
 * <p>This entity is owned by {@link DiscoveredService} and cannot exist independently.
 * It contains the parsed operations and the raw JSON specification.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public class OpenApiSpecification {

    private final String title;
    private final String version;
    private final String rawJson;
    private final List<Operation> operations;
    private final LocalDateTime fetchedAt;

    private OpenApiSpecification(
            final String title,
            final String version,
            final String rawJson,
            final List<Operation> operations,
            final LocalDateTime fetchedAt) {
        this.title = title;
        this.version = version;
        this.rawJson = Objects.requireNonNull(rawJson, "rawJson cannot be null");
        this.operations = operations == null ? List.of() : List.copyOf(operations);
        this.fetchedAt = fetchedAt != null ? fetchedAt : LocalDateTime.now();
    }

    /**
     * Creates a new OpenApiSpecification with the given data.
     *
     * @param title the API title
     * @param version the API version
     * @param rawJson the raw JSON specification
     * @param operations the parsed operations
     * @return a new OpenApiSpecification instance
     */
    public static OpenApiSpecification of(
            final String title,
            final String version,
            final String rawJson,
            final List<Operation> operations) {
        return new OpenApiSpecification(title, version, rawJson, operations, LocalDateTime.now());
    }

    /**
     * Reconstructs an OpenApiSpecification from persisted data.
     *
     * @param title the API title
     * @param version the API version
     * @param rawJson the raw JSON specification
     * @param operations the parsed operations
     * @param fetchedAt when the spec was fetched
     * @return a reconstructed OpenApiSpecification instance
     */
    public static OpenApiSpecification reconstitute(
            final String title,
            final String version,
            final String rawJson,
            final List<Operation> operations,
            final LocalDateTime fetchedAt) {
        return new OpenApiSpecification(title, version, rawJson, operations, fetchedAt);
    }

    /**
     * Returns the API title from the OpenAPI info section.
     *
     * @return the title, may be null
     */
    public String title() {
        return title;
    }

    /**
     * Returns the API version from the OpenAPI info section.
     *
     * @return the version, may be null
     */
    public String version() {
        return version;
    }

    /**
     * Returns the raw JSON specification.
     *
     * @return the raw JSON
     */
    public String rawJson() {
        return rawJson;
    }

    /**
     * Returns all operations in this specification.
     *
     * @return immutable list of operations
     */
    public List<Operation> operations() {
        return operations;
    }

    /**
     * Returns when the specification was fetched.
     *
     * @return the fetch timestamp
     */
    public LocalDateTime fetchedAt() {
        return fetchedAt;
    }

    /**
     * Finds an operation by its unique identifier.
     *
     * @param operationId the operation ID to find
     * @return the operation, or empty if not found
     */
    public Optional<Operation> findOperation(final String operationId) {
        if (operationId == null) {
            return Optional.empty();
        }
        return operations.stream()
            .filter(op -> operationId.equals(op.operationId()))
            .findFirst();
    }

    /**
     * Returns operations filtered by tag.
     *
     * @param tag the tag to filter by, or null/blank for all operations
     * @return list of matching operations
     */
    public List<Operation> operationsByTag(final String tag) {
        if (tag == null || tag.isBlank()) {
            return operations;
        }
        return operations.stream()
            .filter(op -> tag.equals(op.tag()))
            .toList();
    }

    /**
     * Returns the number of operations in this specification.
     *
     * @return the operation count
     */
    public int operationCount() {
        return operations.size();
    }

    /**
     * Returns all unique tags in this specification.
     *
     * @return list of unique tags
     */
    public List<String> tags() {
        return operations.stream()
            .map(Operation::tag)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OpenApiSpecification that = (OpenApiSpecification) o;
        return Objects.equals(title, that.title)
            && Objects.equals(version, that.version)
            && rawJson.equals(that.rawJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, version, rawJson);
    }

    @Override
    public String toString() {
        return "OpenApiSpecification{"
            + "title='" + title + '\''
            + ", version='" + version + '\''
            + ", operations=" + operations.size()
            + '}';
    }
}
