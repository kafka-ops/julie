package kafka.ops.topology.roles;

import kafka.ops.topology.AccessControlProvider;
import kafka.ops.topology.api.mds.MDSApiClient;
import kafka.ops.topology.api.mds.RequestScope;
import java.io.IOException;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RBACProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(RBACProvider.class);
  private final MDSApiClient apiClient;

  public RBACProvider(MDSApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    LOGGER.debug("RBACProvider: createBindings");
    for (TopologyAclBinding binding : bindings) {
      apiClient.bindRequest(binding);
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) {
    LOGGER.debug("RBACProvider: clearAcls");
    bindings.forEach(
        aclBinding -> {
          String principal = aclBinding.getPrincipal();
          String role = aclBinding.getOperation();

          RequestScope scope = new RequestScope();
          scope.setClusters(apiClient.withClusterIDs().forKafka().asMap());
          scope.addResource(
              aclBinding.getResourceType().name(),
              aclBinding.getResourceName(),
              aclBinding.getPattern());
          scope.build();

          apiClient.deleteRole(principal, role, scope);
        });
  }
}
