package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildClusterLevelBinding extends BaseAccessControlAction {

  private final String role;
  private final User user;
  private final Component cmp;
  private final AccessControlProvider controlProvider;

  public BuildClusterLevelBinding(
      AccessControlProvider controlProvider, String role, User user, Component cmp) {
    super();
    this.controlProvider = controlProvider;
    this.role = role;
    this.user = user;
    this.cmp = cmp;
  }

  @Override
  public void run() throws IOException {
    bindings = controlProvider.setClusterLevelRole(role, user.getPrincipal(), cmp);
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
