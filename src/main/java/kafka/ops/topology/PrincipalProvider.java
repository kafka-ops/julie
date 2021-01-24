package kafka.ops.topology;

import kafka.ops.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Set;

public interface PrincipalProvider {

  void configure() throws IOException;

  Set<ServiceAccount> listServiceAccounts() throws IOException;

  ServiceAccount createServiceAccount(String principal, String description) throws IOException;

  void deleteServiceAccount(String principal) throws IOException;
}
