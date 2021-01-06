package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudCLI;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudAclsProvider extends SimpleAclsProvider implements AccessControlProvider {

  private static final Logger LOGGER = LogManager.getLogger(CCloudAclsProvider.class);

  private final CCloudCLI cli;

  public CCloudAclsProvider(
      final TopologyBuilderAdminClient adminClient, final TopologyBuilderConfig config)
      throws IOException {
    super(adminClient);
    this.cli = new CCloudCLI();
    this.cli.setEnvironment(config.getConfluentCloudEnv());
  }

  @Override
  public void createBindings(Set<TopologyAclBinding> bindings) throws IOException {
    try {
      Map<String, ServiceAccount> serviceAccounts = cli.serviceAccounts();
      Set<TopologyAclBinding> ccloudBindings =
          bindings.stream()
              .map(b -> convertToConfluentCloudId(serviceAccounts, b))
              .collect(Collectors.toSet());
      super.createBindings(ccloudBindings);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public void clearBindings(Set<TopologyAclBinding> bindings) throws IOException {
    try {
      Map<String, ServiceAccount> serviceAccounts = cli.serviceAccounts();
      Set<TopologyAclBinding> ccloudBindings =
          bindings.stream()
              .map(b -> convertToConfluentCloudId(serviceAccounts, b))
              .collect(Collectors.toSet());
      super.clearBindings(ccloudBindings);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public Map<String, List<TopologyAclBinding>> listAcls() {
    try {
      Map<Integer, ServiceAccount> serviceAccountsById =
          cli.serviceAccounts().values().stream()
              .collect(Collectors.toMap(ServiceAccount::getId, s -> s));
      Map<String, List<TopologyAclBinding>> map = new HashMap<>();
      super.listAcls()
          .forEach(
              (topic, aclBindings) -> {
                map.put(
                    topic,
                    aclBindings.stream()
                        .map(
                            aclBinding ->
                                convertToServiceAccountName(serviceAccountsById, aclBinding))
                        .collect(Collectors.toList()));
              });
      return map;
    } catch (IOException e) {
      return new HashMap<>();
    }
  }

  private TopologyAclBinding convertToConfluentCloudId(
      Map<String, ServiceAccount> serviceAccounts, TopologyAclBinding binding) {
    if (binding.asAclBinding().isPresent()) {
      return new TopologyAclBinding(
          convertToConfluentCloudId(serviceAccounts, binding.asAclBinding().get()));
    } else {
      String principle = getACLPrinciple(serviceAccounts, binding.getPrincipal());
      return new TopologyAclBinding(
          binding.getResourceType(),
          binding.getResourceName(),
          binding.getHost(),
          binding.getOperation(),
          principle,
          binding.getPattern());
    }
  }

  private AclBinding convertToConfluentCloudId(
      Map<String, ServiceAccount> serviceAccounts, AclBinding aclBinding) {
    AccessControlEntry entry = aclBinding.entry();
    String principle = getACLPrinciple(serviceAccounts, entry.principal());
    AccessControlEntry accessControlEntry =
        new AccessControlEntry(principle, entry.host(), entry.operation(), entry.permissionType());
    return new AclBinding(aclBinding.pattern(), accessControlEntry);
  }

  private TopologyAclBinding convertToServiceAccountName(
      Map<Integer, ServiceAccount> serviceAccounts, TopologyAclBinding binding) {
    if (binding.asAclBinding().isPresent()) {
      return new TopologyAclBinding(
          convertToServiceAccountName(serviceAccounts, binding.asAclBinding().get()));
    } else {
      String serviceAccountName = getName(serviceAccounts, binding.getPrincipal());
      return new TopologyAclBinding(
          binding.getResourceType(),
          binding.getResourceName(),
          binding.getHost(),
          binding.getOperation(),
          serviceAccountName,
          binding.getPattern());
    }
  }

  private AclBinding convertToServiceAccountName(
      Map<Integer, ServiceAccount> serviceAccounts, AclBinding aclBinding) {
    AccessControlEntry entry = aclBinding.entry();
    String serviceAccountName = getName(serviceAccounts, entry.principal());
    AccessControlEntry accessControlEntry =
        new AccessControlEntry(
            serviceAccountName, entry.host(), entry.operation(), entry.permissionType());
    return new AclBinding(aclBinding.pattern(), accessControlEntry);
  }

  private String getACLPrinciple(Map<String, ServiceAccount> serviceAccounts, String name) {
    int id = getId(serviceAccounts, name);
    return id == -1 ? name : "User:" + getId(serviceAccounts, name);
  }

  private int getId(Map<String, ServiceAccount> serviceAccounts, String name) {
    if (serviceAccounts.containsKey(name)) {
      return serviceAccounts.get(name).getId();
    } else {
      try {
        return Integer.parseInt(name);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }

  private String getName(Map<Integer, ServiceAccount> serviceAccounts, String id) {
    try {
      int cCloudId = Integer.parseInt(id.replace("User:", ""));
      return getName(serviceAccounts, cCloudId);
    } catch (NumberFormatException nfe) {
      return id;
    }
  }

  private String getName(Map<Integer, ServiceAccount> serviceAccounts, Integer id) {
    return serviceAccounts.containsKey(id) ? serviceAccounts.get(id).getName() : id.toString();
  }
}
