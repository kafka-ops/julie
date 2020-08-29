package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddConnectorAuthorization implements Action {

  private final Connector connector;
  private final AccessControlProvider controlProvider;
  private List<TopologyAclBinding> bindings;

  public AddConnectorAuthorization(AccessControlProvider controlProvider, Connector connector) {
    this.controlProvider = controlProvider;
    this.connector = connector;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setConnectorAuthorization(
            connector.getPrincipal(), connector.getConnectors().orElse(new ArrayList<>()));
  }

  @Override
  public List<TopologyAclBinding> getBindings() {
    return bindings;
  }
}
