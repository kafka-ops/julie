package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Connector;
import java.io.IOException;

public class SetAclsForKConnect extends BaseAccessControlAction {

  private final Connector app;
  private final String topicPrefix;
  private final AccessControlProvider controlProvider;

  public SetAclsForKConnect(
      AccessControlProvider controlProvider, Connector app, String topicPrefix) {
    super();
    this.app = app;
    this.topicPrefix = topicPrefix;
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() throws IOException {
    bindings = controlProvider.setAclsForConnect(app, topicPrefix);
  }
}
