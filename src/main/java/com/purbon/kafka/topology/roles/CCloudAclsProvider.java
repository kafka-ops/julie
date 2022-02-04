package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudAclsProvider extends SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(CCloudAclsProvider.class);

  private final CCloudApi cli;
  private final String clusterId;
  private Map<String, Long> lookupServiceAccountId;

  public CCloudAclsProvider(
      final TopologyBuilderAdminClient adminClient, final Configuration config) {
    super(adminClient);
    this.cli = new CCloudApi(config.getConfluentCloudClusterUrl(), config);
    this.clusterId = config.getConfluentCloudClusterId();
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    this.lookupServiceAccountId = initializeLookupTable(this.cli);
    for (TopologyAclBinding binding : bindings) {
      TopologyAclBinding translatedBinding = translate(binding);
      cli.createAcl(clusterId, translatedBinding);
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    for (TopologyAclBinding binding : bindings) {
      this.lookupServiceAccountId = initializeLookupTable(this.cli);
      TopologyAclBinding translatedBinding = translate(binding);
      cli.deleteAcls(clusterId, translatedBinding);
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

  private TopologyAclBinding translate(TopologyAclBinding binding) throws IOException {
    String principal = binding.getPrincipal();
    Long translatedPrincipalId = lookupServiceAccountId.get(principal);
    String translatedPrincipal = "";
    if (principal.toLowerCase().startsWith("user")) {
      LOGGER.debug(
              "Translating Confluent Cloud principal "
                      + principal
                      + " to User:"
                      + translatedPrincipalId);
      translatedPrincipal = "User:" + translatedPrincipalId;
    } else if (principal.toLowerCase().startsWith("group")) {
      LOGGER.debug(
              "Translating Confluent Cloud principal "
                      + principal
                      + " to Group:"
                      + translatedPrincipalId);
      translatedPrincipal = "Group:" + translatedPrincipalId;
    } else {
      throw new IOException("Unknown principalType: " + principal);
    }

    TopologyAclBinding translatedBinding =
            TopologyAclBinding.build(
                    binding.getResourceType(),
                    binding.getResourceName(),
                    binding.getHost(),
                    binding.getOperation(),
                    translatedPrincipal,
                    binding.getPattern());
    return translatedBinding;
  }

  private Map<String, Long> initializeLookupTable(CCloudApi cli) throws IOException {
    Map<String, Long> lookupServiceAccountTable = new HashMap<>();

    Map<String, String> lookupSaName = new HashMap<>();
    var v2ServiceAccounts = cli.listServiceAccounts();
    for (var serviceAccount : v2ServiceAccounts) {
      lookupSaName.put(serviceAccount.getId(), serviceAccount.getName());
    }
    var v1ServiceAccounts = cli.listServiceAccountsV1();
    for (var serviceAccount : v1ServiceAccounts) {
      var serviceAccountNameOptional =
          Optional.ofNullable(lookupSaName.get(serviceAccount.getResource_id()));
      serviceAccountNameOptional.ifPresent(
          name -> lookupServiceAccountTable.put(name, serviceAccount.getId()));
    }
    return lookupServiceAccountTable;
  }
}
