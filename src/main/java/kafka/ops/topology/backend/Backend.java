package kafka.ops.topology.backend;

import java.io.IOException;
import java.util.Set;
import kafka.ops.topology.BackendController;
import kafka.ops.topology.model.cluster.ServiceAccount;
import kafka.ops.topology.roles.TopologyAclBinding;

public interface Backend {

  void createOrOpen();

  void createOrOpen(BackendController.Mode mode);

  Set<ServiceAccount> loadServiceAccounts() throws IOException;

  Set<TopologyAclBinding> loadBindings() throws IOException;

  Set<String> loadTopics() throws IOException;

  void saveType(String type);

  void saveBindings(Set<TopologyAclBinding> bindings);

  void saveAccounts(Set<ServiceAccount> accounts);

  void saveTopics(Set<String> topics);

  void close();
}
