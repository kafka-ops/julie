package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

public interface Backend {

  void createOrOpen();

  void createOrOpen(BackendController.Mode mode);

  Set<TopologyAclBinding> load() throws IOException;

  Set<TopologyAclBinding> load(URI uri) throws IOException;

  void saveType(String type);

  void saveBindings(Set<TopologyAclBinding> bindings);

  void close();
}
