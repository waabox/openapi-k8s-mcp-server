-- ============================================================================
-- V1__init_schema.sql
-- Initial database schema for OpenAPI MCP Server (Derby)
-- ============================================================================

-- Table: discovered_services
-- Stores metadata about microservices discovered in Kubernetes
CREATE TABLE discovered_services (
    id                  VARCHAR(255)    NOT NULL PRIMARY KEY,
    namespace           VARCHAR(255)    NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    cluster_ip          VARCHAR(45)     NOT NULL,
    cluster_port        INTEGER         NOT NULL,
    openapi_path        VARCHAR(255)    DEFAULT '/v3/api-docs' NOT NULL,
    status              VARCHAR(50)     DEFAULT 'ACTIVE' NOT NULL,
    discovered_at       TIMESTAMP       NOT NULL,
    last_checked_at     TIMESTAMP,

    CONSTRAINT uk_discovered_services_ns_name UNIQUE (namespace, name)
);

-- Index for namespace queries
CREATE INDEX idx_discovered_services_namespace ON discovered_services(namespace);

-- Index for status queries
CREATE INDEX idx_discovered_services_status ON discovered_services(status);

-- Table: openapi_specifications
-- Stores the OpenAPI specifications for each service
CREATE TABLE openapi_specifications (
    id                  INTEGER         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_id          VARCHAR(255)    NOT NULL,
    title               VARCHAR(500),
    version             VARCHAR(100),
    raw_json            CLOB            NOT NULL,
    operations_json     CLOB            NOT NULL,
    fetched_at          TIMESTAMP       NOT NULL,

    CONSTRAINT fk_openapi_specs_service
        FOREIGN KEY (service_id)
        REFERENCES discovered_services(id)
        ON DELETE CASCADE
);

-- Index for service lookup
CREATE INDEX idx_openapi_specs_service_id ON openapi_specifications(service_id);

-- Unique constraint: one spec per service
CREATE UNIQUE INDEX idx_openapi_specs_unique_service ON openapi_specifications(service_id);
