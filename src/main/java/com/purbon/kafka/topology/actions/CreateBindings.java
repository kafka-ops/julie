package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CreateBindings implements Action {

  private final AccessControlProvider controlProvider;
  private Set<TopologyAclBinding> bindings;

  public CreateBindings(AccessControlProvider controlProvider, Set<TopologyAclBinding> bindings) {
    this.controlProvider = controlProvider;
    this.bindings = bindings;
  }

  @Override
  public void run() throws IOException {
    controlProvider.createBindings(bindings);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Bindings", bindings);
    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
