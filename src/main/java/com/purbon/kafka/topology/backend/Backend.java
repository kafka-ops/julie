package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Set;

public interface Backend {

  void createOrOpen();

  void createOrOpen(BackendController.Mode mode);

  Set<ServiceAccount> loadServiceAccounts() throws IOException;

  Set<TopologyAclBinding> loadBindings() throws IOException;

  void saveType(String type);

  void saveBindings(Set<TopologyAclBinding> bindings);

  void saveAccounts(Set<ServiceAccount> accounts);

  void close();
}
