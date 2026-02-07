package co.fanki.openapimcp.application.command;

import java.util.List;
import java.util.Objects;

/**
 * Command to refresh the discovered services in the cluster.
 *
 * <p>This command triggers a full refresh of all Kubernetes services,
 * fetching their OpenAPI specifications and updating the database.</p>
 *
 * @author waabox(emiliano[at]fanki[dot]co)
 */
public final class RefreshServicesCommand {

    private final List<String> namespaceFilter;
    private final boolean forceRefresh;

    private RefreshServicesCommand(
            final List<String> namespaceFilter,
            final boolean forceRefresh) {
        this.namespaceFilter = namespaceFilter == null ? List.of() : List.copyOf(namespaceFilter);
        this.forceRefresh = forceRefresh;
    }

    /**
     * Creates a command to refresh all namespaces.
     *
     * @return a new RefreshServicesCommand
     */
    public static RefreshServicesCommand all() {
        return new RefreshServicesCommand(List.of(), false);
    }

    /**
     * Creates a command to refresh specific namespaces.
     *
     * @param namespaces the namespaces to refresh
     * @return a new RefreshServicesCommand
     */
    public static RefreshServicesCommand forNamespaces(final List<String> namespaces) {
        Objects.requireNonNull(namespaces, "namespaces cannot be null");
        return new RefreshServicesCommand(namespaces, false);
    }

    /**
     * Creates a forced refresh command (ignores caching).
     *
     * @return a new RefreshServicesCommand with force flag
     */
    public static RefreshServicesCommand forced() {
        return new RefreshServicesCommand(List.of(), true);
    }

    /**
     * Returns the namespace filter.
     *
     * @return list of namespaces to refresh, empty for all
     */
    public List<String> namespaceFilter() {
        return namespaceFilter;
    }

    /**
     * Checks if this is a forced refresh.
     *
     * @return true if forced
     */
    public boolean forceRefresh() {
        return forceRefresh;
    }

    /**
     * Checks if a specific namespace should be included.
     *
     * @param namespace the namespace to check
     * @return true if included
     */
    public boolean includesNamespace(final String namespace) {
        return namespaceFilter.isEmpty() || namespaceFilter.contains(namespace);
    }

    @Override
    public String toString() {
        return "RefreshServicesCommand{"
            + "namespaces=" + namespaceFilter
            + ", force=" + forceRefresh
            + '}';
    }
}
