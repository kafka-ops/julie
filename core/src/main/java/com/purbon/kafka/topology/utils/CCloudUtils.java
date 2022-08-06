package com.purbon.kafka.topology.utils;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import com.purbon.kafka.topology.model.users.ConfluentCloudPrincipal;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudUtils {

  private static final Logger LOGGER = LogManager.getLogger(CCloudUtils.class);
  private static final long SERVICE_ACCOUNT_NOT_FOUND = -1L;

  private Configuration config;

  public CCloudUtils(Configuration config) {
    this.config = config;
  }

  public TopologyAclBinding translateIfNecessary(
      TopologyAclBinding binding, Map<String, Long> serviceAccountIdByNameMap) throws IOException {

    if (!config.isConfluentCloudServiceAccountTranslationEnabled()) {
      LOGGER.debug("Confluent Cloud Principal translation is currently disabled");
      return binding;
    }

    LOGGER.info(
        "At the time of this PR, 4 Feb the Confluent Cloud ACL(s) api require to translate "
            + "Service Account names into ID(s). At some point in time this will not be required anymore, "
            + "so you can configure this out by using ccloud.service_account.translation.enabled=false (true by default)");

    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString(binding.getPrincipal());
    long numericServiceAccountId =
        serviceAccountIdByNameMap.getOrDefault(binding.getPrincipal(), SERVICE_ACCOUNT_NOT_FOUND);

    if (numericServiceAccountId
        == SERVICE_ACCOUNT_NOT_FOUND) { // Translation failed, so we can't continue
      throw new IOException(
          "Translation of principal "
              + principal
              + " failed, please review your system configuration");
    }

    LOGGER.debug(
        "Translating Confluent Cloud principal "
            + binding.getPrincipal()
            + " to "
            + principal.getPrincipalType().name()
            + ":"
            + numericServiceAccountId);
    TopologyAclBinding translatedBinding =
        TopologyAclBinding.build(
            binding.getResourceType(),
            binding.getResourceName(),
            binding.getHost(),
            binding.getOperation(),
            principal.toMappedPrincipalString(numericServiceAccountId),
            binding.getPattern());
    return translatedBinding;
  }

  public Map<String, Long> initializeLookupTable(CCloudApi cli) throws IOException {
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
