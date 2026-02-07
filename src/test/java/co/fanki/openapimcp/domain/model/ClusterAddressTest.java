package co.fanki.openapimcp.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ClusterAddress}.
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
class ClusterAddressTest {

    @Test
    void whenCreatingClusterAddress_givenValidIpAndPort_shouldCreateInstance() {
        // Arrange
        final String ip = "10.0.0.1";
        final int port = 8080;

        // Act
        final ClusterAddress address = ClusterAddress.of(ip, port);

        // Assert
        assertEquals(ip, address.ip());
        assertEquals(port, address.port());
    }

    @Test
    void whenCreatingClusterAddress_givenNullIp_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> ClusterAddress.of(null, 8080));
    }

    @Test
    void whenCreatingClusterAddress_givenBlankIp_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ClusterAddress.of("  ", 8080));
    }

    @Test
    void whenCreatingClusterAddress_givenPortBelowRange_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ClusterAddress.of("10.0.0.1", 0));
    }

    @Test
    void whenCreatingClusterAddress_givenPortAboveRange_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> ClusterAddress.of("10.0.0.1", 70000));
    }

    @Test
    void whenBuildingUrl_givenPathWithSlash_shouldReturnCorrectUrl() {
        // Arrange
        final ClusterAddress address = ClusterAddress.of("10.0.0.1", 8080);

        // Act
        final String url = address.toUrl("/api/users");

        // Assert
        assertEquals("http://10.0.0.1:8080/api/users", url);
    }

    @Test
    void whenBuildingUrl_givenPathWithoutSlash_shouldAddSlash() {
        // Arrange
        final ClusterAddress address = ClusterAddress.of("10.0.0.1", 8080);

        // Act
        final String url = address.toUrl("api/users");

        // Assert
        assertEquals("http://10.0.0.1:8080/api/users", url);
    }

    @Test
    void whenBuildingUrl_givenNullPath_shouldReturnBaseUrl() {
        // Arrange
        final ClusterAddress address = ClusterAddress.of("10.0.0.1", 8080);

        // Act
        final String url = address.toUrl(null);

        // Assert
        assertEquals("http://10.0.0.1:8080/", url);
    }

    @Test
    void whenGettingBaseUrl_shouldReturnUrlWithoutPath() {
        // Arrange
        final ClusterAddress address = ClusterAddress.of("192.168.1.100", 3000);

        // Act
        final String baseUrl = address.baseUrl();

        // Assert
        assertEquals("http://192.168.1.100:3000", baseUrl);
    }

    @Test
    void whenComparingAddresses_givenSameValues_shouldBeEqual() {
        // Arrange
        final ClusterAddress addr1 = ClusterAddress.of("10.0.0.1", 8080);
        final ClusterAddress addr2 = ClusterAddress.of("10.0.0.1", 8080);

        // Assert
        assertEquals(addr1, addr2);
        assertEquals(addr1.hashCode(), addr2.hashCode());
    }
}
