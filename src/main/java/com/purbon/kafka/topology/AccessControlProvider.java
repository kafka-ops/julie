package com.purbon.kafka.topology;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccessControlProvider {

  void createBindings(Set<TopologyAclBinding> bindings) throws IOException;

  void clearBindings(Set<TopologyAclBinding> bindings) throws IOException;

  Map<String, List<TopologyAclBinding>> listAcls();
}
