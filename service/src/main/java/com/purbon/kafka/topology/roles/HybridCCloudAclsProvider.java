package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.adminclient.AclBuilder;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import com.purbon.kafka.topology.utils.CCloudUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    var mayBeTranslated =
        bindings.stream()
            .map(
                binding -> {
                  try {
                    return cCloudUtils.translateIfNecessary(binding, serviceAccountIdByNameMap);
                  } catch (IOException e) {
                    LOGGER.error(e);
                    return binding;
                  }
                })
            .map(
                binding -> {
                  var aclBinding =
                      new AclBuilder(binding.getPrincipal())
                          .addResource(
                              ResourceType.fromString(binding.getResourceType()),
                              binding.getResourceName(),
                              PatternType.fromString(binding.getPattern()))
                          .addControlEntry(
                              binding.getHost(),
                              AclOperation.fromString(binding.getOperation()),
                              AclPermissionType.ALLOW)
                          .build();
                  return new TopologyAclBinding(aclBinding);
                })
            .collect(Collectors.toSet());
    LOGGER.debug(
        "May be translated bindings: "
            + mayBeTranslated.stream()
                .map(TopologyAclBinding::getPrincipal)
                .collect(Collectors.joining(",")));
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
    return super.listAcls();
  }
}
