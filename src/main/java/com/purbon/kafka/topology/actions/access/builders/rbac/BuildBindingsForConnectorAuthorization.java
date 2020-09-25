package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Connector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForConnectorAuthorization extends BaseAccessControlAction {

  private final Connector connector;
  private final AccessControlProvider controlProvider;

  public BuildBindingsForConnectorAuthorization(
      AccessControlProvider controlProvider, Connector connector) {
    super();
    this.controlProvider = controlProvider;
    this.connector = connector;
  }

  @Override
  public void run() {
    bindings =
        controlProvider.setConnectorAuthorization(
            connector.getPrincipal(), connector.getConnectors().orElse(new ArrayList<>()));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("clusterId", connector.getCluster_id());
    return map;
  }
}
