package kafka.ops.topology.actions.access.builders.rbac;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.model.Component;
import kafka.ops.topology.model.User;

public class BuildClusterLevelBinding extends BaseAccessControlAction {

  private final String role;
  private final User user;
  private final Component cmp;
  private final BindingsBuilderProvider builderProvider;

  public BuildClusterLevelBinding(
      BindingsBuilderProvider builderProvider, String role, User user, Component cmp) {
    super();
    this.builderProvider = builderProvider;
    this.role = role;
    this.user = user;
    this.cmp = cmp;
  }

  @Override
  protected void execute() throws IOException {
    bindings = builderProvider.setClusterLevelRole(role, user.getPrincipal(), cmp);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Role", role);
    map.put("Principal", user.getPrincipal());
    map.put("Component", cmp);
    return map;
  }
}
