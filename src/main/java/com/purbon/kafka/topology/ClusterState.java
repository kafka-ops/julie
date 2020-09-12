package com.purbon.kafka.topology;

import com.purbon.kafka.topology.clusterstate.FileStateProcessor;
import com.purbon.kafka.topology.clusterstate.StateProcessor;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClusterState {

  private static final String STORE_TYPE = "acls";

  private final StateProcessor stateProcessor;
  private Set<TopologyAclBinding> bindings;

  public ClusterState() {
    this(new FileStateProcessor());
  }

  public ClusterState(StateProcessor stateProcessor) {
    this.stateProcessor = stateProcessor;
    this.bindings = new HashSet<>();
  }

  public void add(List<TopologyAclBinding> bindings) {
    this.bindings.addAll(bindings);
  }

  public void add(TopologyAclBinding binding) {
    this.bindings.add(binding);
  }

  public Set<TopologyAclBinding> getBindings() {
    return new HashSet<>(bindings);
  }

  public void flushAndClose() {
    stateProcessor.createOrOpen();
    stateProcessor.saveType(STORE_TYPE);
    stateProcessor.saveBindings(bindings);
    stateProcessor.close();
  }

  public void load() throws IOException {
    stateProcessor.createOrOpen();
    bindings.addAll(stateProcessor.load());
  }

  public void reset() {
    bindings.clear();
  }

  public int size() {
    return bindings.size();
  }
}
