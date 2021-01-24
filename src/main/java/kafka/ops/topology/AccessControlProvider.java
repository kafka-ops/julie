package kafka.ops.topology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kafka.ops.topology.roles.TopologyAclBinding;

public interface AccessControlProvider {

  void createBindings(Set<TopologyAclBinding> bindings) throws IOException;

  void clearBindings(Set<TopologyAclBinding> bindings) throws IOException;

  default Map<String, List<TopologyAclBinding>> listAcls() {
    return new HashMap<>();
  }
}
