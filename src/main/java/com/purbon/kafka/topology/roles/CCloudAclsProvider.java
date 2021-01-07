package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloudCLI;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.*;
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
              .filter(Objects::nonNull)
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
              .filter(Objects::nonNull)
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
      AclBinding aclBinding = convertToConfluentCloudId(serviceAccounts, binding.asAclBinding().get());
      return aclBinding == null ? null : new TopologyAclBinding(aclBinding);
    } else {
      Integer id = getId(serviceAccounts, binding.getPrincipal());
      return id == null ? null :  getTopologyAclBinding(binding, "User:" + id);
    }
  }

  private AclBinding convertToConfluentCloudId(
      Map<String, ServiceAccount> serviceAccounts, AclBinding aclBinding) {
    Integer id = getId(serviceAccounts, aclBinding.entry().principal());
    return id == null ? null : getAclBinding(aclBinding, "User:" + id);
  }

  private TopologyAclBinding convertToServiceAccountName(
      Map<Integer, ServiceAccount> serviceAccounts, TopologyAclBinding binding) {
    if (binding.asAclBinding().isPresent()) {
      return new TopologyAclBinding(
          convertToServiceAccountName(serviceAccounts, binding.asAclBinding().get()));
    } else {
      String serviceAccountName = getName(serviceAccounts, binding.getPrincipal());
      return getTopologyAclBinding(binding, serviceAccountName);
    }
  }

  private AclBinding convertToServiceAccountName(
      Map<Integer, ServiceAccount> serviceAccounts, AclBinding aclBinding) {
    String serviceAccountName = getName(serviceAccounts, aclBinding.entry().principal());
    return getAclBinding(aclBinding, serviceAccountName);
  }

  private AclBinding getAclBinding(AclBinding aclBinding, String principle) {
    AccessControlEntry entry = aclBinding.entry();
    AccessControlEntry accessControlEntry = new AccessControlEntry(principle, entry.host(), entry.operation(), entry.permissionType());
    return new AclBinding(aclBinding.pattern(), accessControlEntry);
  }

  private TopologyAclBinding getTopologyAclBinding(TopologyAclBinding binding, String principle) {
    return new TopologyAclBinding(
            binding.getResourceType(),
            binding.getResourceName(),
            binding.getHost(),
            binding.getOperation(),
            principle,
            binding.getPattern());
  }

  private Integer getId(Map<String, ServiceAccount> serviceAccounts, String name) {
    if (serviceAccounts.containsKey(name)) {
      return serviceAccounts.get(name).getId();
    } else {
      try {
        return Integer.parseInt(name);
      } catch (NumberFormatException e) {
        return null;
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
