package co.fanki.openapimcp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Operation}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class OperationTest {

    @Test
    void whenBuildingOperation_givenRequiredFields_shouldCreateInstance() {
        // Act
        final Operation operation = Operation.builder()
            .operationId("getUser")
            .method("GET")
            .path("/users/{id}")
            .build();

        // Assert
        assertEquals("getUser", operation.operationId());
        assertEquals("GET", operation.method());
        assertEquals("/users/{id}", operation.path());
    }

    @Test
    void whenBuildingOperation_givenLowercaseMethod_shouldConvertToUppercase() {
        // Act
        final Operation operation = Operation.builder()
            .operationId("createUser")
            .method("post")
            .path("/users")
            .build();

        // Assert
        assertEquals("POST", operation.method());
    }

    @Test
    void whenBuildingOperation_givenAllFields_shouldSetAllProperties() {
        // Arrange
        final List<OperationParameter> params = List.of(
            OperationParameter.pathParam("id", "{\"type\":\"string\"}")
        );

        // Act
        final Operation operation = Operation.builder()
            .operationId("getUserById")
            .method("GET")
            .path("/users/{id}")
            .summary("Get a user by ID")
            .description("Retrieves a user from the database")
            .tag("Users")
            .parameters(params)
            .requestBodySchema(null)
            .responseSchema("{\"type\":\"object\"}")
            .build();

        // Assert
        assertEquals("getUserById", operation.operationId());
        assertEquals("Get a user by ID", operation.summary());
        assertEquals("Retrieves a user from the database", operation.description());
        assertEquals("Users", operation.tag());
        assertEquals(1, operation.parameters().size());
        assertEquals("{\"type\":\"object\"}", operation.responseSchema());
    }

    @Test
    void whenCheckingRequestBody_givenOperationWithBody_shouldReturnTrue() {
        // Arrange
        final Operation operation = Operation.builder()
            .operationId("createUser")
            .method("POST")
            .path("/users")
            .requestBodySchema("{\"type\":\"object\"}")
            .build();

        // Assert
        assertTrue(operation.hasRequestBody());
    }

    @Test
    void whenCheckingRequestBody_givenOperationWithoutBody_shouldReturnFalse() {
        // Arrange
        final Operation operation = Operation.builder()
            .operationId("getUser")
            .method("GET")
            .path("/users/{id}")
            .build();

        // Assert
        assertFalse(operation.hasRequestBody());
    }

    @Test
    void whenGettingPathParameters_givenMixedParams_shouldReturnOnlyPathParams() {
        // Arrange
        final List<OperationParameter> params = List.of(
            OperationParameter.pathParam("id", "{\"type\":\"string\"}"),
            OperationParameter.queryParam("includeDetails", "{\"type\":\"boolean\"}"),
            OperationParameter.pathParam("version", "{\"type\":\"integer\"}")
        );

        final Operation operation = Operation.builder()
            .operationId("getResource")
            .method("GET")
            .path("/api/{version}/resources/{id}")
            .parameters(params)
            .build();

        // Act
        final List<OperationParameter> pathParams = operation.pathParameters();

        // Assert
        assertEquals(2, pathParams.size());
        assertTrue(pathParams.stream().allMatch(OperationParameter::isPathParam));
    }

    @Test
    void whenGettingQueryParameters_givenMixedParams_shouldReturnOnlyQueryParams() {
        // Arrange
        final List<OperationParameter> params = List.of(
            OperationParameter.pathParam("id", "{\"type\":\"string\"}"),
            OperationParameter.queryParam("page", "{\"type\":\"integer\"}"),
            OperationParameter.queryParam("size", "{\"type\":\"integer\"}")
        );

        final Operation operation = Operation.builder()
            .operationId("listResources")
            .method("GET")
            .path("/resources/{id}/items")
            .parameters(params)
            .build();

        // Act
        final List<OperationParameter> queryParams = operation.queryParameters();

        // Assert
        assertEquals(2, queryParams.size());
        assertTrue(queryParams.stream().allMatch(OperationParameter::isQueryParam));
    }

    @Test
    void whenComparingOperations_givenSameOperationId_shouldBeEqual() {
        // Arrange
        final Operation op1 = Operation.builder()
            .operationId("getUser")
            .method("GET")
            .path("/users/{id}")
            .build();

        final Operation op2 = Operation.builder()
            .operationId("getUser")
            .method("GET")
            .path("/users/{id}")
            .build();

        // Assert
        assertEquals(op1, op2);
        assertEquals(op1.hashCode(), op2.hashCode());
    }
}
