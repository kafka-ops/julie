package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;

public class SetPredefinedRole extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final String principal;
  private final String predefinedRole;
  private final String topicPrefix;

  public SetPredefinedRole(
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
}
