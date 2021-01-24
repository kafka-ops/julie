package kafka.ops.topology.serviceAccounts;

import kafka.ops.topology.PrincipalProvider;
import kafka.ops.topology.TopologyBuilderConfig;
import kafka.ops.topology.api.ccloud.CCloudCLI;
import kafka.ops.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CCloudPrincipalProvider implements PrincipalProvider {

  private CCloudCLI cCloudCLI;
  private String env;

  public CCloudPrincipalProvider(TopologyBuilderConfig config) {
    this.cCloudCLI = new CCloudCLI();
    this.env = config.getConfluentCloudEnv();
  }

  @Override
  public void configure() throws IOException {
    cCloudCLI.setEnvironment(env);
  }

  @Override
  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    return new HashSet<>(cCloudCLI.serviceAccounts().values());
  }

  @Override
  public ServiceAccount createServiceAccount(String principal, String description)
      throws IOException {
    return cCloudCLI.newServiceAccount(principal, description);
  }

  @Override
  public void deleteServiceAccount(String principal) throws IOException {
    Map<String, ServiceAccount> accounts = cCloudCLI.serviceAccounts();
    ServiceAccount sa = accounts.get(principal);
    cCloudCLI.deleteServiceAccount(sa.getId());
  }
}
