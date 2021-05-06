package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RbacResourceType;
import com.purbon.kafka.topology.api.mds.RequestScope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
              aclBinding.getResourceType(), aclBinding.getResourceName(), aclBinding.getPattern());
          scope.build();

          apiClient.deleteRole(principal, role, scope);
        });
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    Map<String, List<TopologyAclBinding>> map = new HashMap<>();
    List<String> roleNames = apiClient.getRoleNames();
    for (String roleName : roleNames) {
      List<String> principalNames = apiClient.lookupKafkaPrincipalsByRoleForKafka(roleName);
      for (String principalName : principalNames) {
        List<RbacResourceType> resources =
            apiClient.lookupResourcesForKafka(principalName, roleName);
        for (RbacResourceType resource : resources) {
          if (!map.containsKey(resource.getName())) {
            map.put(resource.getName(), new ArrayList<>());
          }
          TopologyAclBinding binding =
              TopologyAclBinding.build(
                  normalize(resource.getResourceType()),
                  resource.getName(),
                  "*",
                  roleName,
                  principalName,
                  resource.getPatternType());
          map.get(resource.getName()).add(binding);
        }
      }
    }
    return map;
  }

  private String normalize(String resourceType) {
    String[] fields = resourceType.split("(?=\\p{Upper})");
    return String.join("_", fields).toUpperCase();
  }
}
