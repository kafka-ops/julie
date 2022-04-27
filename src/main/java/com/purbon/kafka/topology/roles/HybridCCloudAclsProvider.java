package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import com.purbon.kafka.topology.utils.CCloudUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HybridCCloudAclsProvider extends SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(HybridCCloudAclsProvider.class);

  private final CCloudApi cli;
  private final String clusterId;
  private final Configuration config;
  private CCloudUtils cCloudUtils;

  public HybridCCloudAclsProvider(
      final TopologyBuilderAdminClient adminClient, final Configuration config) throws IOException {
    super(adminClient);
    this.cli = new CCloudApi(config.getConfluentCloudClusterUrl(), config);
    this.clusterId = config.getConfluentCloudClusterId();
    this.config = config;
    this.cCloudUtils = new CCloudUtils(config);
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    var serviceAccountIdByNameMap = cCloudUtils.initializeLookupTable(this.cli);
    var mayBeTranslated = bindings
            .stream()
            .map(binding -> {
      try {
        return cCloudUtils.translateIfNecessary(binding, serviceAccountIdByNameMap);
      } catch (IOException e) {
        LOGGER.error(e);
        return binding;
      }
    }).collect(Collectors.toSet());
    super.createBindings(mayBeTranslated);
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    var serviceAccountIdByNameMap = cCloudUtils.initializeLookupTable(this.cli);
    for (TopologyAclBinding binding : bindings) {
      adminClient.clearAcls(cCloudUtils.translateIfNecessary(binding, serviceAccountIdByNameMap));
    }
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    try {
      Map<String, List<TopologyAclBinding>> bindings = new HashMap<>();
      for (TopologyAclBinding binding : cli.listAcls(clusterId)) {
        String resourceName = binding.getResourceName();
        if (!bindings.containsKey(resourceName)) {
          bindings.put(resourceName, new ArrayList<>());
        }
        bindings.get(resourceName).add(binding);
      }
      return bindings;
    } catch (IOException e) {
      LOGGER.warn(e);
      return Collections.emptyMap();
    }
  }
}
