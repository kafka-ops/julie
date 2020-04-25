package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface StateProcessor {

  void createOrOpen();

  List<TopologyAclBinding> load() throws IOException;

  List<TopologyAclBinding> load(URI uri) throws IOException;

  void saveType(String type);

  void saveBindings(List<TopologyAclBinding> bindings);

  void close();
}
