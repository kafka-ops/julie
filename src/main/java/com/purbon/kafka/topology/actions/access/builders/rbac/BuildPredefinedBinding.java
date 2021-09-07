package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BuildPredefinedBinding extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final String principal;
  private final String predefinedRole;
  private final String topicPrefix;

  public BuildPredefinedBinding(
      BindingsBuilderProvider builderProvider,
      String principal,
      String predefinedRole,
      String topicPrefix) {
    super();
    this.builderProvider = builderProvider;
    this.principal = principal;
    this.predefinedRole = predefinedRole;
    this.topicPrefix = topicPrefix;
  }

  @Override
  protected void execute() {
    TopologyAclBinding binding =
        builderProvider.setPredefinedRole(principal, predefinedRole, topicPrefix);
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
