package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuildPredefinedBinding extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final String principal;
  private final String predefinedRole;
  private final String topicPrefix;

  public BuildPredefinedBinding(
      AccessControlProvider controlProvider,
      String principal,
      String predefinedRole,
      String topicPrefix) {
    super();
    this.controlProvider = controlProvider;
    this.principal = principal;
    this.predefinedRole = predefinedRole;
    this.topicPrefix = topicPrefix;
  }

  @Override
  public void run() throws IOException {
    TopologyAclBinding binding =
        controlProvider.setPredefinedRole(principal, predefinedRole, topicPrefix);
    bindings = Collections.singletonList(binding);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", principal);
    map.put("Role", predefinedRole);
    map.put("Topic", topicPrefix);
    return map;
  }
}
