package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClearAcls implements Action {

  private final AccessControlProvider controlProvider;
  private final Set<TopologyAclBinding> bindingsForRemoval;

  public ClearAcls(
      AccessControlProvider controlProvider, Set<TopologyAclBinding> bindingsForRemoval) {
    this.controlProvider = controlProvider;
    this.bindingsForRemoval = bindingsForRemoval;
  }

  @Override
  public void run() {
    controlProvider.clearAcls(bindingsForRemoval);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
