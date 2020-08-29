package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.User;
import java.io.IOException;

public class SetClusterLevelRole extends BaseAccessControlAction {

  private final String role;
  private final User user;
  private final Component cmp;
  private final AccessControlProvider controlProvider;

  public SetClusterLevelRole(
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
}
