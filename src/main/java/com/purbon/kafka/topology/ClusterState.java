package com.purbon.kafka.topology;

import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.StateProcessor;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ClusterState {

  private static final String STORE_TYPE = "acls";

  private final StateProcessor stateProcessor;
  private Set<TopologyAclBinding> bindings;

  public ClusterState() {
    this(new FileSateProcessor());
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

  public void forEachBinding(Consumer<? super TopologyAclBinding> action) {
    bindings.forEach(action);
  }

  public void flushAndClose() {
    stateProcessor.createOrOpen();
    stateProcessor.saveType(STORE_TYPE);
    stateProcessor.saveBindings(bindings);
    stateProcessor.close();
  }

  public void load() throws IOException {
    bindings = stateProcessor.load();
  }

  public void reset() {
    bindings.clear();
  }

  public int size() {
    return bindings.size();
  }
}
