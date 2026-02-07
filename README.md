![CI](https://github.com/waabox/sql-mcp-server/actions/workflows/ci.yml/badge.svg) ![Java](https://img.shields.io/badge/Java-21-blue.svg) ![Maven](https://img.shields.io/badge/Maven-3.9-orange.svg) [![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](./LICENSE) ![Docker Image](https://img.shields.io/badge/docker-ready-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen) ![MCP Compatible](https://img.shields.io/badge/MCP-Server-blueviolet)
# OpenAPI MCP Server for Kubernetes

A lightweight MCP (Model Context Protocol) server that runs inside your Kubernetes cluster, crawls your Services and endpoints via the Kubernetes API, auto-detects exposed OpenAPI specs, and builds a live, queryable index of your entire microservice surface area. It normalizes each spec (derefs, merges, fixes inconsistencies), tracks versions, and exposes the whole thing as MCP tools so agents can introspect, list, and invoke endpoints directly over in-cluster networking — essentially turning your microservices into a distributed, self-describing function registry.

## Features

- **Automatic Discovery**: Discovers services in Kubernetes namespaces
- **OpenAPI Parsing**: Fetches and parses OpenAPI/Swagger specifications
- **MCP Integration**: Exposes tools via Model Context Protocol
- **Embedded Database**: Uses Apache Derby (no external DB required)
- **Scheduled Refresh**: Configurable auto-refresh (default: every 10 minutes)

## Status

| Feature | Status |
|---------|--------|
| Service discovery via K8s API | ✅ Done |
| OpenAPI 3.x spec fetching & parsing | ✅ Done |
| Persistence in embedded Derby | ✅ Done |
| Basic MCP tools (list, get, invoke) | ✅ Done |
| Rate limiting & exponential backoff | ✅ Done |
| Configurable URL templates | ✅ Done |
| OpenAPI 2.0 / Swagger support | 🚧 Planned |
| Authentication to target services | 🚧 Planned |
| MCP authentication / role-based access | 🚧 Planned |
| Minimal status UI | 💭 Maybe later |

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

## Kubernetes Deployment

The `k8s/` directory contains all the manifests needed to deploy the MCP server inside a Kubernetes cluster.

### Prerequisites

- Kubernetes cluster (1.24+)
- `kubectl` configured with cluster access
- Container registry access (to push/pull the image)

### Build and Push Image

```bash
# Build the image
docker build -t your-registry/openapi-mcp-server:latest .

# Push to your registry
docker push your-registry/openapi-mcp-server:latest
```

### Deploy to Kubernetes

```bash
# Create namespace and RBAC
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/rbac.yaml

# Create configuration
kubectl apply -f k8s/configmap.yaml

# Deploy the server
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Or apply all at once:

```bash
kubectl apply -f k8s/
```

### Manifest Overview

| File | Description |
|------|-------------|
| `namespace.yaml` | Creates the `openapi-mcp` namespace |
| `rbac.yaml` | ServiceAccount, ClusterRole, and ClusterRoleBinding for service discovery |
| `configmap.yaml` | Environment configuration (namespaces, paths, scheduler) |
| `deployment.yaml` | Main server deployment with health checks |
| `service.yaml` | ClusterIP service for internal access |

### RBAC Permissions

The server requires the following permissions to discover services:

```yaml
rules:
  - apiGroups: [""]
    resources: ["services"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["namespaces"]
    verbs: ["get", "list"]
  - apiGroups: [""]
    resources: ["endpoints"]
    verbs: ["get", "list"]
```

### Configuration

Update the ConfigMap to customize behavior:

```yaml
# k8s/configmap.yaml
data:
  K8S_IN_CLUSTER: "true"
  K8S_NAMESPACE_FILTER: "production,staging"  # Filter specific namespaces
  OPENAPI_URL_TEMPLATE: "http://{service-name}.{namespace}.svc.cluster.local/v3/api-docs"
  SCHEDULER_INTERVAL_MS: "300000"  # 5 minutes
```

### Update Image Reference

Edit `k8s/deployment.yaml` to use your container registry:

```yaml
containers:
  - name: openapi-mcp-server
    image: your-registry/openapi-mcp-server:latest
```

### Verify Deployment

```bash
# Check pod status
kubectl get pods -n openapi-mcp

# View logs
kubectl logs -n openapi-mcp -l app.kubernetes.io/name=openapi-mcp-server

# Check service discovery is working
kubectl exec -n openapi-mcp deploy/openapi-mcp-server -- curl -s localhost:8080/health
```

### In-Cluster DNS Resolution

When running inside the cluster, use DNS-based URL templates for reliable service discovery:

```bash
# Kubernetes internal DNS pattern
OPENAPI_URL_TEMPLATE=http://{service-name}.{namespace}.svc.cluster.local/v3/api-docs
```

This allows the MCP server to resolve service endpoints using Kubernetes DNS without needing external routing.

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DERBY_DB_PATH` | `./data/openapi_mcp` | Path to Derby database |
| `K8S_NAMESPACE_FILTER` | _(all)_ | Filter services by namespace |
| `OPENAPI_PATH` | `/v3/api-docs` | Default OpenAPI spec path |
| `OPENAPI_URL_TEMPLATE` | _(none)_ | Custom URL template for OpenAPI discovery |
| `DISCOVERY_LABEL` | _(none)_ | Label selector for discovery |
| `K8S_IN_CLUSTER` | `false` | Run in-cluster mode |
| `KUBECONFIG` | `~/.kube/config` | Path to kubeconfig |
| `SCHEDULER_ENABLED` | `true` | Enable auto-refresh |
| `SCHEDULER_INTERVAL_MS` | `600000` | Refresh interval (10 min) |
| `OPENAPI_MAX_CONCURRENT_REQUESTS` | `10` | Max concurrent OpenAPI fetch requests |
| `OPENAPI_BACKOFF_MAX_FAILURES` | `3` | Failures before applying backoff |
| `OPENAPI_BACKOFF_BASE_SECONDS` | `60` | Base backoff duration (doubles each failure) |
| `OPENAPI_BACKOFF_MAX_SECONDS` | `3600` | Maximum backoff duration (1 hour cap) |

### Scaling for Large Clusters (200+ services)

For large clusters, the server includes built-in protection mechanisms:

**Rate Limiting**: Limits concurrent HTTP requests to OpenAPI endpoints to avoid overwhelming the network or services. Configure with `OPENAPI_MAX_CONCURRENT_REQUESTS`.

**Exponential Backoff**: Services that fail repeatedly are temporarily skipped. After `OPENAPI_BACKOFF_MAX_FAILURES` consecutive failures, the service enters backoff mode. The backoff duration doubles with each additional failure, starting at `OPENAPI_BACKOFF_BASE_SECONDS` and capped at `OPENAPI_BACKOFF_MAX_SECONDS`.

Example configuration for a 500-service cluster:

```yaml
# k8s/configmap.yaml
data:
  OPENAPI_MAX_CONCURRENT_REQUESTS: "20"      # More parallelism
  OPENAPI_BACKOFF_MAX_FAILURES: "5"          # More tolerant
  OPENAPI_BACKOFF_BASE_SECONDS: "120"        # 2 min base backoff
  OPENAPI_BACKOFF_MAX_SECONDS: "7200"        # 2 hour max backoff
  SCHEDULER_INTERVAL_MS: "300000"            # Refresh every 5 min
```

### OpenAPI URL Template

By default, the server fetches OpenAPI specs using the cluster IP and port: `http://{cluster-ip}:{port}/{openapi-path}`.

You can override this with a custom URL template using placeholders:

```bash
OPENAPI_URL_TEMPLATE=http://{service-name}.svc.example.com/{service-name}/v3/api-docs
```

**Supported placeholders:**

| Placeholder | Description |
|-------------|-------------|
| `{service-name}` | Kubernetes service name |
| `{namespace}` | Kubernetes namespace |
| `{cluster-ip}` | Service cluster IP |
| `{port}` | Service port |

**Examples:**

```bash
# DNS-based discovery
OPENAPI_URL_TEMPLATE=http://{service-name}.{namespace}.svc.cluster.local/v3/api-docs

# Custom domain with service name in path
OPENAPI_URL_TEMPLATE=http://{service-name}.svc.example.com/{service-name}/v3/api-docs

# External gateway
OPENAPI_URL_TEMPLATE=https://api.example.com/{namespace}/{service-name}/v3/api-docs
```

### Kubernetes Service Annotations

Mark your services for OpenAPI discovery:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
  annotations:
    openapi.mcp.io/enabled: "true"        # Enable discovery
    openapi.mcp.io/path: "/v3/api-docs"   # Custom OpenAPI path
spec:
  ports:
    - port: 8080
      name: http  # Port named 'http' is preferred
```

To disable discovery:

```yaml
annotations:
  openapi.mcp.io/enabled: "false"
```

## Using the MCP

The MCP server should be deployed in your Kubernetes cluster first. Then connect your local AI tools to it via port-forward or ingress.

### Step 1: Deploy to Kubernetes

```bash
kubectl apply -f k8s/
```

### Step 2: Connect to the MCP Server

**Option A: In-Cluster (recommended)**

If your AI agents run inside the same cluster, use the internal DNS:

```
http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp
```

**Option B: Ingress (external access)**

Expose the service via ingress for external AI tools:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openapi-mcp-server
  namespace: openapi-mcp
spec:
  rules:
    - host: openapi-mcp.your-cluster.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: openapi-mcp-server
                port:
                  number: 80
```

**Option C: Port Forward (local development)**

```bash
kubectl port-forward -n openapi-mcp svc/openapi-mcp-server 8080:80
# Then use http://localhost:8080/mcp
```

### Step 3: Configure Your AI Tool

#### Claude Code (CLI)

```bash
# External access
claude mcp add openapi-k8s --url http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp

# Or in-cluster
claude mcp add openapi-k8s --url http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp
```

Or edit `.claude/settings.json`:

```json
{
  "mcpServers": {
    "openapi-k8s": {
      "url": "http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp"
    }
  }
}
```

#### Claude Desktop

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "openapi-k8s": {
      "url": "http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp"
    }
  }
}
```

#### VS Code

Add to your `settings.json` or `.vscode/mcp.json`:

```json
{
  "mcp.servers": {
    "openapi-k8s": {
      "url": "http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp"
    }
  }
}
```

#### Cursor

**macOS**: `~/.cursor/mcp.json`
**Windows**: `%USERPROFILE%\.cursor\mcp.json`

```json
{
  "mcpServers": {
    "openapi-k8s": {
      "url": "http://openapi-mcp-server.openapi-mcp.svc.cluster.local/mcp"
    }
  }
}
```

### Verify Connection

Once configured, you can ask the AI to:

- "List all discovered services"
- "Show me the operations for the user-service"
- "Invoke the GET /users endpoint on user-service"

## MCP Tools

### list_services

Lists all discovered microservices.

**Request:**
```json
{
  "namespace": "production"
}
```

**Response:**
```json
{
  "services": [
    {
      "id": "production/user-service",
      "name": "user-service",
      "namespace": "production",
      "status": "ACTIVE",
      "operationCount": 12
    },
    {
      "id": "production/order-service",
      "name": "order-service",
      "namespace": "production",
      "status": "ACTIVE",
      "operationCount": 8
    }
  ]
}
```

### get_operations

Gets all operations from a service.

**Request:**
```json
{
  "service_id": "production/user-service",
  "tag": "users"
}
```

**Response:**
```json
{
  "operations": [
    {
      "operationId": "getUsers",
      "method": "GET",
      "path": "/api/users",
      "summary": "List all users",
      "tags": ["users"]
    },
    {
      "operationId": "getUserById",
      "method": "GET",
      "path": "/api/users/{id}",
      "summary": "Get user by ID",
      "tags": ["users"]
    },
    {
      "operationId": "createUser",
      "method": "POST",
      "path": "/api/users",
      "summary": "Create a new user",
      "tags": ["users"]
    }
  ]
}
```

### get_operation_details

Gets detailed information about an operation including parameters and request body schema.

**Request:**
```json
{
  "service_id": "production/user-service",
  "operation_id": "createUser"
}
```

**Response:**
```json
{
  "operationId": "createUser",
  "method": "POST",
  "path": "/api/users",
  "summary": "Create a new user",
  "description": "Creates a new user in the system",
  "tags": ["users"],
  "parameters": [],
  "requestBody": {
    "required": true,
    "content": {
      "application/json": {
        "schema": {
          "type": "object",
          "properties": {
            "name": { "type": "string" },
            "email": { "type": "string", "format": "email" },
            "role": { "type": "string", "enum": ["admin", "user"] }
          },
          "required": ["name", "email"]
        }
      }
    }
  }
}
```

### invoke_endpoint

Invokes a microservice endpoint directly through in-cluster networking.

**Request:**
```json
{
  "service_id": "production/user-service",
  "operation_id": "createUser",
  "body": {
    "name": "John Doe",
    "email": "john@example.com",
    "role": "user"
  }
}
```

**Response:**
```json
{
  "status": 201,
  "headers": {
    "content-type": "application/json",
    "x-request-id": "abc-123"
  },
  "body": {
    "id": "usr_abc123",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "user",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**With path and query parameters:**
```json
{
  "service_id": "production/order-service",
  "operation_id": "getOrdersByUser",
  "path_params": {
    "userId": "usr_abc123"
  },
  "query_params": {
    "status": "pending",
    "limit": 10
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

By default, the server uses Apache Derby as an embedded database (no external DB required). Data is stored in `DERBY_DB_PATH`.

### Using a Different Database

You can switch to any JDBC-compatible database (PostgreSQL, MySQL, etc.) by updating the configuration.

#### PostgreSQL Example

1. **Add the PostgreSQL driver to `pom.xml`:**

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. **Update `application.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:openapi_mcp}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
```

3. **Set environment variables in Kubernetes:**

```yaml
# k8s/configmap.yaml
data:
  DB_HOST: "postgres.openapi-mcp.svc.cluster.local"
  DB_PORT: "5432"
  DB_NAME: "openapi_mcp"
  DB_USER: "postgres"

# Use a Secret for the password
```

4. **Rebuild and deploy:**

```bash
mvn clean package -DskipTests
docker build -t your-registry/openapi-mcp-server:latest .
docker push your-registry/openapi-mcp-server:latest
kubectl rollout restart deployment/openapi-mcp-server -n openapi-mcp
```

Flyway will automatically run the migrations on startup. The schema is database-agnostic, so it works with any JDBC-compatible database.

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

## Security Considerations

This server is designed to run **inside your Kubernetes cluster** and provides direct access to invoke microservice endpoints. Consider the following:

### Network Isolation

- Deploy in a dedicated namespace with restricted network policies
- The server should only be accessible from trusted sources (other in-cluster services or via authenticated ingress)
- Use `K8S_NAMESPACE_FILTER` to limit which namespaces the server can discover

### Principle of Least Privilege

```yaml
# Restrict to specific namespaces
K8S_NAMESPACE_FILTER: "production,staging"

# Avoid exposing internal/system namespaces
# Never include: kube-system, kube-public, cert-manager, etc.
```

### invoke_endpoint Risks

The `invoke_endpoint` tool allows AI agents to call any discovered endpoint. This is powerful but potentially dangerous:

- **No authentication passthrough yet**: The server calls endpoints as itself (service-to-service), not as the original user
- **No rate limiting on invocations**: An agent could theoretically spam endpoints
- **Full request body control**: The agent constructs the request payload

### Recommendations

1. **Start with read-only namespaces**: Test with namespaces that only have GET endpoints
2. **Use NetworkPolicies**: Restrict which services the MCP server can reach
3. **Monitor usage**: Log all `invoke_endpoint` calls for audit
4. **Plan for auth**: Future versions will support token-based access control for MCP clients

### Planned Security Features

| Feature | Status |
|---------|--------|
| MCP client authentication | 🚧 Planned |
| Per-tool authorization (read vs write) | 🚧 Planned |
| Endpoint allowlist/blocklist | 🚧 Planned |
| Request signing / audit log | 🚧 Planned |

## License

MIT

---

@author waabox(emiliano[at]fanki[dot]co)
