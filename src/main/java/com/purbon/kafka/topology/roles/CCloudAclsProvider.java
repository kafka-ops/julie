package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudAclsProvider extends SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(CCloudAclsProvider.class);

  private final CCloudApi cli;
  private final String clusterId;

  public CCloudAclsProvider(
      final TopologyBuilderAdminClient adminClient, final Configuration config) throws IOException {
    super(adminClient);
    this.cli = new CCloudApi(config.getConfluentCloudClusterUrl(), config);
    this.clusterId = config.getConfluentCloudClusterId();
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    for (TopologyAclBinding binding : bindings) {
      cli.createAcl(clusterId, binding);
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    for (TopologyAclBinding binding : bindings) {
      cli.deleteAcls(clusterId, binding);
    }
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    return Collections.emptyMap();
  }
}
