package co.fanki.openapimcp.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ServiceId}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class ServiceIdTest {

    @Test
    void whenCreatingServiceId_givenValidNamespaceAndName_shouldCreateInstance() {
        // Arrange
        final String namespace = "default";
        final String name = "my-service";

        // Act
        final ServiceId serviceId = ServiceId.of(namespace, name);

        // Assert
        assertEquals(namespace, serviceId.namespace());
        assertEquals(name, serviceId.name());
    }

    @Test
    void whenCreatingServiceId_givenNullNamespace_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> ServiceId.of(null, "my-service"));
    }

    @Test
    void whenCreatingServiceId_givenNullName_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> ServiceId.of("default", null));
    }

    @Test
    void whenCreatingServiceId_givenBlankNamespace_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ServiceId.of("  ", "my-service"));
    }

    @Test
    void whenCreatingServiceId_givenBlankName_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ServiceId.of("default", "  "));
    }

    @Test
    void whenCallingAsString_givenValidServiceId_shouldReturnFormattedString() {
        // Arrange
        final ServiceId serviceId = ServiceId.of("production", "api-gateway");

        // Act
        final String result = serviceId.asString();

        // Assert
        assertEquals("production/api-gateway", result);
    }

    @Test
    void whenParsingFromString_givenValidFormat_shouldCreateServiceId() {
        // Arrange
        final String value = "staging/user-service";

        // Act
        final ServiceId serviceId = ServiceId.fromString(value);

        // Assert
        assertEquals("staging", serviceId.namespace());
        assertEquals("user-service", serviceId.name());
    }

    @Test
    void whenParsingFromString_givenInvalidFormat_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ServiceId.fromString("invalid-format"));
    }

    @Test
    void whenParsingFromString_givenNull_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> ServiceId.fromString(null));
    }

    @Test
    void whenComparingServiceIds_givenSameValues_shouldBeEqual() {
        // Arrange
        final ServiceId id1 = ServiceId.of("default", "my-service");
        final ServiceId id2 = ServiceId.of("default", "my-service");

        // Assert
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void whenComparingServiceIds_givenDifferentValues_shouldNotBeEqual() {
        // Arrange
        final ServiceId id1 = ServiceId.of("default", "service-a");
        final ServiceId id2 = ServiceId.of("default", "service-b");

        // Assert
        assertNotEquals(id1, id2);
    }

    @Test
    void whenComparingServiceIds_givenDifferentNamespaces_shouldNotBeEqual() {
        // Arrange
        final ServiceId id1 = ServiceId.of("production", "my-service");
        final ServiceId id2 = ServiceId.of("staging", "my-service");

        // Assert
        assertNotEquals(id1, id2);
    }
}
