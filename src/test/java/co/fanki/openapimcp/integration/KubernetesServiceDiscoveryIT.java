package co.fanki.openapimcp.integration;

import co.fanki.openapimcp.infrastructure.kubernetes.KubernetesServiceDiscovery;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for Kubernetes service discovery using Testcontainers with k3s.
 *
 * <p>This test uses nginx with OpenResty/Lua to serve both static OpenAPI JSON
 * and dynamic endpoints like /api/do-ping that returns "pong".</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KubernetesServiceDiscoveryIT {

    private static final Logger LOG = LoggerFactory.getLogger(
        KubernetesServiceDiscoveryIT.class
    );

    private static final String K3S_IMAGE = "rancher/k3s:v1.28.5-k3s1";
    private static final String TEST_NAMESPACE = "test-ns";
    private static final int DEPLOYMENT_READY_TIMEOUT_SECONDS = 90;

    private K3sContainer k3sContainer;
    private ApiClient apiClient;
    private String testAppClusterIp;
    private KubernetesServiceDiscovery discovery;

    @BeforeAll
    void setUp() throws Exception {
        LOG.info("=== Starting Integration Test ===");

        startK3sCluster();
        deployTestApp();
        waitForDeploymentAndGetClusterIp();
        setupDiscoveryService();

        LOG.info("=== Setup Complete ===");
    }

    @AfterAll
    void tearDown() {
        if (k3sContainer != null) {
            k3sContainer.stop();
        }
    }

    @Test
    @Order(1)
    void whenDiscoveringServices_givenApiDeployed_shouldFindTestApiService() {
        LOG.info("Test 1: Service discovery...");

        final List<KubernetesServiceDiscovery.DiscoveredK8sService> services =
            discovery.discoverServices();

        final var testService = services.stream()
            .filter(s -> "test-api".equals(s.name()))
            .findFirst()
            .orElse(null);

        assertNotNull(testService, "Should find test-api service");
        assertEquals(TEST_NAMESPACE, testService.namespace());
        assertEquals(80, testService.port());
        assertEquals("/v3/api-docs", testService.openApiPath());

        LOG.info("PASS: Found test-api at {}", testService.clusterIp());
    }

    @Test
    @Order(2)
    void whenFetchingOpenApi_shouldContainDoPingOperation() throws Exception {
        LOG.info("Test 2: OpenAPI spec...");

        final var result = k3sContainer.execInContainer(
            "wget", "-qO-", "http://" + testAppClusterIp + "/v3/api-docs"
        );

        final String spec = result.getStdout();
        assertTrue(spec.contains("doPing"), "Should contain doPing");
        assertTrue(spec.contains("/api/do-ping"), "Should contain path");

        LOG.info("PASS: OpenAPI contains doPing");
    }

    @Test
    @Order(3)
    void whenInvokingDoPing_shouldReturnPong() throws Exception {
        LOG.info("Test 3: /api/do-ping endpoint...");

        final var result = k3sContainer.execInContainer(
            "wget", "-qO-", "http://" + testAppClusterIp + "/api/do-ping"
        );

        assertEquals("pong", result.getStdout().trim());
        LOG.info("PASS: /api/do-ping returned 'pong'");
    }

    private void startK3sCluster() throws Exception {
        LOG.info("Starting k3s...");
        k3sContainer = new K3sContainer(DockerImageName.parse(K3S_IMAGE));
        k3sContainer.start();
        apiClient = Config.fromConfig(new StringReader(k3sContainer.getKubeConfigYaml()));
        LOG.info("k3s ready");
    }

    private void deployTestApp() throws Exception {
        LOG.info("Deploying test-api...");

        // All-in-one manifest using openresty (nginx with Lua)
        final String manifest = """
            ---
            apiVersion: v1
            kind: Namespace
            metadata:
              name: test-ns
            ---
            apiVersion: v1
            kind: ConfigMap
            metadata:
              name: nginx-config
              namespace: test-ns
            data:
              nginx.conf: |
                events { worker_connections 64; }
                http {
                  server {
                    listen 80;

                    location /v3/api-docs {
                      default_type application/json;
                      return 200 '{"openapi":"3.0.1","info":{"title":"Test API","version":"1.0.0"},"paths":{"/api/do-ping":{"get":{"operationId":"doPing","summary":"Ping","responses":{"200":{"description":"OK"}}}}}}';
                    }

                    location /api/do-ping {
                      default_type text/plain;
                      return 200 'pong';
                    }

                    location /api/health {
                      default_type text/plain;
                      return 200 'OK';
                    }
                  }
                }
            ---
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: test-api
              namespace: test-ns
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: test-api
              template:
                metadata:
                  labels:
                    app: test-api
                spec:
                  containers:
                    - name: nginx
                      image: nginx:alpine
                      ports:
                        - containerPort: 80
                      volumeMounts:
                        - name: config
                          mountPath: /etc/nginx/nginx.conf
                          subPath: nginx.conf
                      readinessProbe:
                        httpGet:
                          path: /api/health
                          port: 80
                        initialDelaySeconds: 2
                        periodSeconds: 2
                  volumes:
                    - name: config
                      configMap:
                        name: nginx-config
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: test-api
              namespace: test-ns
              annotations:
                openapi.fanki.co/enabled: "true"
                openapi.fanki.co/path: "/v3/api-docs"
            spec:
              selector:
                app: test-api
              ports:
                - port: 80
                  targetPort: 80
                  name: http
            """;

        k3sContainer.execInContainer("sh", "-c",
            "cat > /tmp/manifest.yaml << 'ENDOFFILE'\n" + manifest + "\nENDOFFILE"
        );

        final var result = k3sContainer.execInContainer(
            "kubectl", "apply", "-f", "/tmp/manifest.yaml"
        );

        if (result.getExitCode() != 0) {
            throw new RuntimeException("Deploy failed: " + result.getStderr());
        }

        LOG.info("Deployed");
    }

    private void waitForDeploymentAndGetClusterIp() throws Exception {
        LOG.info("Waiting for deployment...");

        final AppsV1Api appsApi = new AppsV1Api(apiClient);
        final CoreV1Api coreApi = new CoreV1Api(apiClient);
        final long timeout = TimeUnit.SECONDS.toMillis(DEPLOYMENT_READY_TIMEOUT_SECONDS);
        final long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            try {
                final V1Deployment dep = appsApi.readNamespacedDeployment(
                    "test-api", TEST_NAMESPACE
                ).execute();

                if (isReady(dep)) {
                    final var svc = coreApi.readNamespacedService(
                        "test-api", TEST_NAMESPACE
                    ).execute();

                    testAppClusterIp = svc.getSpec().getClusterIP();
                    LOG.info("Ready. ClusterIP: {}", testAppClusterIp);

                    // Wait for pod ready
                    waitForPod(coreApi);
                    return;
                }
            } catch (final Exception ignored) {
            }
            Thread.sleep(2000);
        }

        throw new RuntimeException("Timeout");
    }

    private void waitForPod(final CoreV1Api coreApi) throws Exception {
        final long timeout = TimeUnit.SECONDS.toMillis(30);
        final long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            final V1PodList pods = coreApi.listNamespacedPod(TEST_NAMESPACE)
                .labelSelector("app=test-api").execute();

            if (!pods.getItems().isEmpty()) {
                final V1Pod pod = pods.getItems().get(0);
                if (pod.getStatus() != null && pod.getStatus().getConditions() != null) {
                    boolean ready = pod.getStatus().getConditions().stream()
                        .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
                    if (ready) {
                        return;
                    }
                }
            }
            Thread.sleep(1000);
        }
    }

    private boolean isReady(final V1Deployment dep) {
        if (dep.getStatus() == null || dep.getStatus().getConditions() == null) {
            return false;
        }
        return dep.getStatus().getConditions().stream()
            .anyMatch(c -> "Available".equals(c.getType()) && "True".equals(c.getStatus()));
    }

    private void setupDiscoveryService() throws Exception {
        final var factory = new TestableClientFactory(apiClient);
        discovery = new KubernetesServiceDiscovery(factory);

        final var f1 = discovery.getClass().getDeclaredField("defaultOpenApiPath");
        f1.setAccessible(true);
        f1.set(discovery, "/v3/api-docs");

        final var f2 = discovery.getClass().getDeclaredField("namespaceFilter");
        f2.setAccessible(true);
        f2.set(discovery, TEST_NAMESPACE);
    }

    private static class TestableClientFactory
            extends co.fanki.openapimcp.infrastructure.kubernetes.KubernetesClientFactory {
        private final ApiClient client;

        TestableClientFactory(final ApiClient c) {
            this.client = c;
        }

        @Override
        public void init() {
        }

        @Override
        public ApiClient getApiClient() {
            return client;
        }
    }
}
