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
  public static final long SERVICE_ACCOUNT_NOT_FOUND = -1L;

  private final CCloudApi cli;
  private final String clusterId;
  private final Configuration config;
  private Map<String, Long> serviceAccountIdByName;

  public CCloudAclsProvider(
      final TopologyBuilderAdminClient adminClient, final Configuration config) {
    super(adminClient);
    this.cli = new CCloudApi(config.getConfluentCloudClusterUrl(), config);
    this.clusterId = config.getConfluentCloudClusterId();
    this.config = config;
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    this.serviceAccountIdByName = initializeLookupTable(this.cli);
    for (TopologyAclBinding binding : bindings) {
      cli.createAcl(clusterId, translateIfNecessary(binding));
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    for (TopologyAclBinding binding : bindings) {
      this.serviceAccountIdByName = initializeLookupTable(this.cli);
      cli.deleteAcls(clusterId, translateIfNecessary(binding));
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

  private TopologyAclBinding translateIfNecessary(TopologyAclBinding binding) throws IOException {

    if (!config.isConfluentCloudServiceAccountTranslationEnabled()) {
      LOGGER.debug("Confluent Cloud Principal translation is currently disabled");
      return binding;
    }

    LOGGER.info(
        "At the time of this PR, 4 Feb the Confluent Cloud ACL(s) api require to translate "
            + "Service Account names into ID(s). At some point in time this will not be required anymore, "
            + "so you can configure this out by using ccloud.service_account.translation.enabled=false (true by default)");

    Principal principal = Principal.fromString(binding.getPrincipal());
    Long numericServiceAccountId = serviceAccountIdByName.getOrDefault(principal.serviceAccountName, SERVICE_ACCOUNT_NOT_FOUND);
    if (numericServiceAccountId == SERVICE_ACCOUNT_NOT_FOUND) { // Translation failed, so we can't continue
      throw new IOException(
          "Translation of principal "
              + principal
              + " failed, please review your system configuration");
    }

    TopologyAclBinding translatedBinding =
        TopologyAclBinding.build(
            binding.getResourceType(),
            binding.getResourceName(),
            binding.getHost(),
            binding.getOperation(),
            mappedPrincipal(
                principal.principalType.name(), binding.getPrincipal(), numericServiceAccountId),
            binding.getPattern());
    return translatedBinding;
  }

  private String mappedPrincipal(String type, String principal, Long translatedPrincipalId) {
    LOGGER.debug(
        "Translating Confluent Cloud principal "
            + principal
            + " to "
            + type
            + ":"
            + translatedPrincipalId);
    return type + ":" + translatedPrincipalId;
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
