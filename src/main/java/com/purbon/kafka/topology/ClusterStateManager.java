package com.purbon.kafka.topology;

import com.purbon.kafka.topology.clusterstate.ClusterState;
import com.purbon.kafka.topology.clusterstate.StateProcessor;
import com.purbon.kafka.topology.clusterstate.YAMLStateProcessor;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClusterStateManager {

  public static final String CLUSTER_STATE_FILENAME = ".cluster-state";

  private final StateProcessor stateProcessor;
  private final FileReader stateFileReader;
  private final FileWriter stateFileWriter;
  private ClusterState clusterState;

  public ClusterStateManager() {
    this(new YAMLStateProcessor());
  }

  public ClusterStateManager(StateProcessor stateProcessor) {
    try {
      this.stateFileReader = new FileReader(CLUSTER_STATE_FILENAME);
      this.stateFileWriter = new FileWriter(CLUSTER_STATE_FILENAME);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.stateProcessor = stateProcessor;
    this.clusterState = new ClusterState();
  }

  public void update(List<TopologyAclBinding> bindings) {
    this.clusterState.setAclBindings(bindings);
  }

  public void forEachBinding(Consumer<? super TopologyAclBinding> action) {
    this.clusterState.getAclBindings().forEach(action);
  }

  public void flushAndClose() {
    this.stateProcessor.writeState(this.stateFileWriter, this.clusterState);
  }

  public void load() {
    this.clusterState.setAclBindings(
        this.stateProcessor.readState(this.stateFileReader).getAclBindings());
  }

  public void reset() {
    this.clusterState.setAclBindings(new ArrayList<>());
  }

  public int size() {
    return this.clusterState.getAclBindings().size();
  }
}
