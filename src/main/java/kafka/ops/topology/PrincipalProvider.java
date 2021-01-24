package kafka.ops.topology;

import java.io.IOException;
import java.util.Set;
import kafka.ops.topology.model.cluster.ServiceAccount;

public interface PrincipalProvider {

  void configure() throws IOException;

  Set<ServiceAccount> listServiceAccounts() throws IOException;

  ServiceAccount createServiceAccount(String principal, String description) throws IOException;

  void deleteServiceAccount(String principal) throws IOException;
}
