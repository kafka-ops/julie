package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddConnectorAuthorization implements Action {

  private final Connector connector;
  private final AccessControlProvider controlProvider;
  private List<TopologyAclBinding> bindings;

  public AddConnectorAuthorization(AccessControlProvider controlProvider, Connector connector) {
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
  public List<TopologyAclBinding> getBindings() {
    return bindings;
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("clusterId", connector.getCluster_id());
    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
