package kafka.ops.topology.actions.access.builders.rbac;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.roles.TopologyAclBinding;

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
  protected void execute() throws IOException {
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
