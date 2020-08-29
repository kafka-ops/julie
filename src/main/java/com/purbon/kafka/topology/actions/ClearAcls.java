package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.ClusterState;
import java.io.IOException;

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
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    return sb.toString();
  }
}
