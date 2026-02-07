# OpenAPI MCP Server for Kubernetes

MCP (Model Context Protocol) Server that discovers microservices running in Kubernetes, fetches their OpenAPI specifications, and exposes tools for listing and invoking endpoints.

## Features

- **Automatic Discovery**: Discovers services in Kubernetes namespaces
- **OpenAPI Parsing**: Fetches and parses OpenAPI/Swagger specifications
- **MCP Integration**: Exposes tools via Model Context Protocol
- **Embedded Database**: Uses Apache Derby (no external DB required)
- **Scheduled Refresh**: Automatically refreshes specs every 10 minutes

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                        MCP Server                              │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │ K8s Discovery│  │OpenAPI Parser│  │  MCP Tools Layer   │    │
│  │   Service    │──│   Service    │──│                    │    │
│  └──────────────┘  └──────────────┘  │ - list_services    │    │
│         │                 │          │ - get_operations   │    │
│         ▼                 ▼          │ - invoke_endpoint  │    │
│  ┌──────────────────────────────┐    │ - get_op_details   │    │
│  │      Derby (Embedded)        │    └────────────────────┘    │
│  │  - discovered_services       │                              │
│  │  - openapi_specifications    │                              │
│  └──────────────────────────────┘                              │
│         ▲                                                      │
│  ┌──────────────┐                                              │
│  │  Scheduler   │ ← Refresh every 10 minutes                   │
│  └──────────────┘                                              │
└───────────────────────────────────────────────────────────────-┘
```

## Requirements

- Java 21+
- Docker (for running tests)
- Kubernetes cluster access (kubeconfig or in-cluster)

## Quick Start

### Build

```bash
mvn clean package
```

### Run

```bash
# With local kubeconfig
java -jar target/openapi-mcp-server-*.jar

# With custom config
KUBECONFIG=/path/to/config java -jar target/openapi-mcp-server-*.jar
```

### Docker

```bash
docker build -t openapi-mcp-server .
docker run -v ~/.kube/config:/root/.kube/config:ro openapi-mcp-server
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DERBY_DB_PATH` | `./data/openapi_mcp` | Path to Derby database |
| `K8S_NAMESPACE_FILTER` | _(all)_ | Filter services by namespace |
| `OPENAPI_PATH` | `/v3/api-docs` | Default OpenAPI spec path |
| `DISCOVERY_LABEL` | _(none)_ | Label selector for discovery |
| `K8S_IN_CLUSTER` | `false` | Run in-cluster mode |
| `KUBECONFIG` | `~/.kube/config` | Path to kubeconfig |
| `SCHEDULER_ENABLED` | `true` | Enable auto-refresh |
| `SCHEDULER_INTERVAL_MS` | `600000` | Refresh interval (10 min) |

### Kubernetes Service Annotations

Mark your services for OpenAPI discovery:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
  annotations:
    openapi.fanki.co/enabled: "true"        # Enable discovery
    openapi.fanki.co/path: "/v3/api-docs"   # Custom OpenAPI path
spec:
  ports:
    - port: 8080
      name: http  # Port named 'http' is preferred
```

To disable discovery:

```yaml
annotations:
  openapi.fanki.co/enabled: "false"
```

## MCP Tools

### list_services

Lists all discovered microservices.

```json
{
  "name": "list_services",
  "inputSchema": {
    "type": "object",
    "properties": {
      "namespace": { "type": "string" }
    }
  }
}
```

### get_operations

Gets all operations from a service.

```json
{
  "name": "get_operations",
  "inputSchema": {
    "type": "object",
    "properties": {
      "service_id": { "type": "string" },
      "tag": { "type": "string" }
    },
    "required": ["service_id"]
  }
}
```

### get_operation_details

Gets detailed information about an operation.

```json
{
  "name": "get_operation_details",
  "inputSchema": {
    "type": "object",
    "properties": {
      "service_id": { "type": "string" },
      "operation_id": { "type": "string" }
    },
    "required": ["service_id", "operation_id"]
  }
}
```

### invoke_endpoint

Invokes a microservice endpoint.

```json
{
  "name": "invoke_endpoint",
  "inputSchema": {
    "type": "object",
    "properties": {
      "service_id": { "type": "string" },
      "operation_id": { "type": "string" },
      "path_params": { "type": "object" },
      "query_params": { "type": "object" },
      "body": { "type": "object" }
    },
    "required": ["service_id", "operation_id"]
  }
}
```

## Project Structure

```
co.fanki.openapimcp/
├── domain/                    # Domain layer (DDD)
│   ├── model/                 # Entities & Value Objects
│   │   ├── ServiceId.java
│   │   ├── ClusterAddress.java
│   │   ├── DiscoveredService.java
│   │   ├── OpenApiSpecification.java
│   │   └── Operation.java
│   ├── repository/            # Repositories (JDBI)
│   │   ├── DiscoveredServiceRepository.java
│   │   ├── DiscoveredServiceRowMapper.java
│   │   └── OpenApiSpecificationRowMapper.java
│   └── service/               # Domain services
│       ├── OpenApiParser.java
│       └── EndpointInvoker.java
├── application/               # Application layer
│   ├── command/               # Write operations
│   ├── query/                 # Read operations
│   └── service/               # Application services
├── infrastructure/            # Infrastructure layer
│   ├── kubernetes/            # K8s client
│   ├── http/                  # HTTP clients
│   ├── mcp/                   # MCP tools
│   └── scheduling/            # Refresh scheduler
└── config/                    # Spring configuration
```

## Testing

```bash
# Run all tests (unit + integration)
mvn test
```

### Test Summary

| Type | Count | Time |
|------|-------|------|
| Unit Tests | 40 | ~0.1s |
| Integration Tests | 3 | ~33s |
| **Total** | **43** | **~35s** |

The integration tests use Testcontainers with k3s to:
1. Start a real Kubernetes cluster
2. Deploy a test service with OpenAPI
3. Verify service discovery
4. Test `/api/do-ping` endpoint returns "pong"

## Database

Uses Apache Derby as an embedded database. Data is stored in `DERBY_DB_PATH`.

### Schema

```sql
CREATE TABLE discovered_services (
    id              VARCHAR(255) PRIMARY KEY,
    namespace       VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    cluster_ip      VARCHAR(45) NOT NULL,
    cluster_port    INTEGER NOT NULL,
    openapi_path    VARCHAR(255) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    discovered_at   TIMESTAMP NOT NULL,
    last_checked_at TIMESTAMP
);

CREATE TABLE openapi_specifications (
    id              INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    service_id      VARCHAR(255) REFERENCES discovered_services(id),
    title           VARCHAR(500),
    version         VARCHAR(100),
    raw_json        CLOB NOT NULL,
    operations_json CLOB NOT NULL,
    fetched_at      TIMESTAMP NOT NULL
);
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database Access | JDBI 3 |
| Database | Apache Derby (embedded) |
| K8s Client | Kubernetes Client Java 20 |
| OpenAPI Parser | Swagger Parser 2.1 |
| Migrations | Flyway |
| Testing | JUnit 5, Testcontainers, k3s |

## License

MIT

---

@author waabox(emiliano[at]fanki[dot]co)
