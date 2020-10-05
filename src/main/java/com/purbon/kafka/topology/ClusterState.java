package com.purbon.kafka.topology;

import com.purbon.kafka.topology.backend.Backend;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClusterState {

  private static final String STORE_TYPE = "acls";

  private final Backend backend;
  private Set<TopologyAclBinding> bindings;

  public ClusterState() {
    this(new FileBackend());
  }

  public ClusterState(Backend backend) {
    this.backend = backend;
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
    backend.createOrOpen();
    backend.saveType(STORE_TYPE);
    backend.saveBindings(bindings);
    backend.close();
  }

  public void load() throws IOException {
    backend.createOrOpen();
    bindings.addAll(backend.load());
  }

  public void reset() {
    bindings.clear();
  }

  public int size() {
    return bindings.size();
  }
}
