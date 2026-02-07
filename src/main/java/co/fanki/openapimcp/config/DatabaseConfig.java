package co.fanki.openapimcp.config;

import co.fanki.openapimcp.domain.repository.DiscoveredServiceRowMapper;
import co.fanki.openapimcp.domain.repository.OpenApiSpecificationRowMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Database configuration for the application.
 *
 * <p>Configures JDBI3 with custom RowMappers for domain objects.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@Configuration
public class DatabaseConfig {

    /**
     * Configures the ObjectMapper for JSON processing.
     *
     * @return the configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Configures JDBI3 with the DataSource and custom RowMappers.
     *
     * @param dataSource the Spring-managed DataSource
     * @param objectMapper the JSON object mapper
     * @return the configured Jdbi instance
     */
    @Bean
    public Jdbi jdbi(final DataSource dataSource, final ObjectMapper objectMapper) {
        final Jdbi jdbi = Jdbi.create(dataSource);

        // Install SQL Object plugin
        jdbi.installPlugin(new SqlObjectPlugin());

        // Register custom RowMappers
        jdbi.registerRowMapper(new DiscoveredServiceRowMapper());
        jdbi.registerRowMapper(new OpenApiSpecificationRowMapper(objectMapper));

        return jdbi;
    }
}
