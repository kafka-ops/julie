package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(SimpleAclsProvider.class);

  private final TopologyBuilderAdminClient adminClient;

  public SimpleAclsProvider(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    LOGGER.debug("AclsProvider: createBindings");
    List<AclBinding> bindingsAsNativeKafka =
        bindings.stream()
            .filter(binding -> binding.asAclBinding().isPresent())
            .map(binding -> binding.asAclBinding().get())
            .collect(Collectors.toList());
    try {
      adminClient.createAcls(bindingsAsNativeKafka);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    LOGGER.debug("AclsProvider: clearAcls");
    for (TopologyAclBinding binding : bindings) {
      try {
        adminClient.clearAcls(binding);
      } catch (IOException ex) {
        LOGGER.error(ex);
        throw ex;
      }
    }
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    Map<String, List<TopologyAclBinding>> map = new HashMap<>();
    adminClient
        .fetchAclsList()
        .forEach(
            (topic, aclBindings) ->
                map.put(
                    topic,
                    aclBindings.stream()
                        .map(TopologyAclBinding::new)
                        .collect(Collectors.toList())));
    return map;
  }
}
