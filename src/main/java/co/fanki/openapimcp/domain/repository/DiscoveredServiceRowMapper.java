package co.fanki.openapimcp.domain.repository;

import co.fanki.openapimcp.domain.model.ClusterAddress;
import co.fanki.openapimcp.domain.model.DiscoveredService;
import co.fanki.openapimcp.domain.model.OpenApiPath;
import co.fanki.openapimcp.domain.model.ServiceId;
import co.fanki.openapimcp.domain.model.ServiceStatus;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * JDBI RowMapper for {@link DiscoveredService}.
 *
 * <p>Maps database rows to domain objects without the specification.
 * The specification is loaded separately and attached.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public class DiscoveredServiceRowMapper implements RowMapper<DiscoveredService> {

    @Override
    public DiscoveredService map(final ResultSet rs, final StatementContext ctx)
            throws SQLException {

        final ServiceId serviceId = ServiceId.of(
            rs.getString("namespace"),
            rs.getString("name")
        );

        final ClusterAddress address = ClusterAddress.of(
            rs.getString("cluster_ip"),
            rs.getInt("cluster_port")
        );

        final OpenApiPath openApiPath = OpenApiPath.of(
            rs.getString("openapi_path")
        );

        final ServiceStatus status = ServiceStatus.valueOf(
            rs.getString("status")
        );

        final LocalDateTime discoveredAt = toLocalDateTime(
            rs.getTimestamp("discovered_at")
        );

        final LocalDateTime lastCheckedAt = toLocalDateTime(
            rs.getTimestamp("last_checked_at")
        );

        return DiscoveredService.reconstitute(
            serviceId,
            address,
            openApiPath,
            discoveredAt,
            status,
            lastCheckedAt,
            null // Specification loaded separately
        );
    }

    private LocalDateTime toLocalDateTime(final Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
