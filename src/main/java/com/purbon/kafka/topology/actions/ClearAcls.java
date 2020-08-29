package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClearAcls implements Action {

  private final AccessControlProvider controlProvider;
  private final ClusterState clusterState;

  public ClearAcls(AccessControlProvider controlProvider, ClusterState clusterState) {
    this.controlProvider = controlProvider;
    this.clusterState = clusterState;
  }

  @Override
  public void run() throws IOException {
    controlProvider.clearAcls(clusterState);
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
