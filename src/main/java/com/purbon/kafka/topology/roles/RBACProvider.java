package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RBACProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(RBACProvider.class);

  public static final String LITERAL = "LITERAL";
  public static final String PREFIX = "PREFIXED";
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

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    LOGGER.info("Not implemented yet!");
    return new HashMap<>();
  }
}
