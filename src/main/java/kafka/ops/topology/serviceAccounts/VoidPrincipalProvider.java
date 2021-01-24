package kafka.ops.topology.serviceAccounts;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import kafka.ops.topology.PrincipalProvider;
import kafka.ops.topology.model.cluster.ServiceAccount;

public class VoidPrincipalProvider implements PrincipalProvider {

  @Override
  public void configure() throws IOException {
    throw new IOException("Not implemented!!");
  }

  @Override
  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    return Collections.emptySet();
  }

  @Override
  public ServiceAccount createServiceAccount(String principal, String description)
      throws IOException {
    throw new IOException("Not implemented!!");
  }

  @Override
  public void deleteServiceAccount(String principal) throws IOException {
    throw new IOException("Not implemented!!");
  }
}
