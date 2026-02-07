package co.fanki.openapimcp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DiscoveredService}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class DiscoveredServiceTest {

    @Test
    void whenDiscoveringService_givenValidInput_shouldCreateActiveService() {
        // Arrange
        final ServiceId id = ServiceId.of("default", "my-service");
        final ClusterAddress address = ClusterAddress.of("10.0.0.1", 8080);
        final OpenApiPath path = OpenApiPath.defaultPath();

        // Act
        final DiscoveredService service = DiscoveredService.discover(id, address, path);

        // Assert
        assertEquals(id, service.id());
        assertEquals(address, service.address());
        assertEquals(path, service.openApiPath());
        assertEquals(ServiceStatus.ACTIVE, service.status());
        assertFalse(service.hasSpecification());
    }

    @Test
    void whenAttachingSpecification_givenValidSpec_shouldMarkAsActive() {
        // Arrange
        final DiscoveredService service = createTestService();
        final OpenApiSpecification spec = createTestSpecification();

        // Act
        service.attachSpecification(spec);

        // Assert
        assertTrue(service.hasSpecification());
        assertEquals(ServiceStatus.ACTIVE, service.status());
        assertTrue(service.specification().isPresent());
    }

    @Test
    void whenMarkingUnreachable_shouldUpdateStatus() {
        // Arrange
        final DiscoveredService service = createTestService();

        // Act
        service.markUnreachable();

        // Assert
        assertEquals(ServiceStatus.UNREACHABLE, service.status());
        assertFalse(service.isActive());
    }

    @Test
    void whenMarkingNoOpenApi_shouldClearSpecification() {
        // Arrange
        final DiscoveredService service = createTestService();
        service.attachSpecification(createTestSpecification());

        // Act
        service.markNoOpenApi();

        // Assert
        assertEquals(ServiceStatus.NO_OPENAPI, service.status());
        assertFalse(service.hasSpecification());
    }

    @Test
    void whenFindingOperation_givenValidOperationId_shouldReturnOperation() {
        // Arrange
        final DiscoveredService service = createTestService();
        service.attachSpecification(createTestSpecification());

        // Act
        final Optional<Operation> result = service.findOperation("getUsers");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("getUsers", result.get().operationId());
    }

    @Test
    void whenFindingOperation_givenInvalidOperationId_shouldReturnEmpty() {
        // Arrange
        final DiscoveredService service = createTestService();
        service.attachSpecification(createTestSpecification());

        // Act
        final Optional<Operation> result = service.findOperation("nonExistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void whenFindingOperation_givenNoSpecification_shouldReturnEmpty() {
        // Arrange
        final DiscoveredService service = createTestService();

        // Act
        final Optional<Operation> result = service.findOperation("getUsers");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void whenGettingOperations_givenTag_shouldFilterByTag() {
        // Arrange
        final DiscoveredService service = createTestService();
        service.attachSpecification(createTestSpecification());

        // Act
        final List<Operation> result = service.operations("Users");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Users", result.get(0).tag());
    }

    @Test
    void whenBuildingEndpointUrl_givenOperation_shouldReturnFullUrl() {
        // Arrange
        final DiscoveredService service = createTestService();
        final Operation operation = Operation.builder()
            .operationId("getUser")
            .method("GET")
            .path("/api/users/{id}")
            .build();

        // Act
        final String url = service.buildEndpointUrl(operation);

        // Assert
        assertEquals("http://10.0.0.1:8080/api/users/{id}", url);
    }

    @Test
    void whenGettingOpenApiUrl_shouldReturnCorrectUrl() {
        // Arrange
        final DiscoveredService service = createTestService();

        // Act
        final String url = service.openApiUrl();

        // Assert
        assertEquals("http://10.0.0.1:8080/v3/api-docs", url);
    }

    private DiscoveredService createTestService() {
        return DiscoveredService.discover(
            ServiceId.of("default", "test-service"),
            ClusterAddress.of("10.0.0.1", 8080),
            OpenApiPath.defaultPath()
        );
    }

    private OpenApiSpecification createTestSpecification() {
        final List<Operation> operations = List.of(
            Operation.builder()
                .operationId("getUsers")
                .method("GET")
                .path("/users")
                .tag("Users")
                .build(),
            Operation.builder()
                .operationId("createOrder")
                .method("POST")
                .path("/orders")
                .tag("Orders")
                .build()
        );

        return OpenApiSpecification.of(
            "Test API",
            "1.0.0",
            "{\"openapi\":\"3.0.0\"}",
            operations
        );
    }
}
