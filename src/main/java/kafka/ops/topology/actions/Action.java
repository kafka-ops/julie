package kafka.ops.topology.actions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import kafka.ops.topology.roles.TopologyAclBinding;

public interface Action {

  void run() throws IOException;

  default List<TopologyAclBinding> getBindings() {
    return Collections.emptyList();
  }
}
