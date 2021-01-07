package com.purbon.kafka.topology.serviceAccounts;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.ccloud.CCloud;
import com.purbon.kafka.topology.api.ccloud.CCloudCLI;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CCloudPrincipalProvider implements PrincipalProvider {

  private CCloud cCloud;

  public CCloudPrincipalProvider(CCloud cCloud) throws IOException {
    if (cCloud == null) {
      throw new IllegalArgumentException("CCloud cannot be null for this provider. " + getClass().getName());
    }
    this.cCloud = cCloud;
  }

  @Override
  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    return new HashSet<>(cCloud.serviceAccounts().values());
  }

  @Override
  public ServiceAccount createServiceAccount(String principal, String description)
      throws IOException {
    return cCloud.newServiceAccount(principal, description);
  }

  @Override
  public void deleteServiceAccount(String principal) throws IOException {
    Map<String, ServiceAccount> accounts = cCloud.serviceAccounts();
    ServiceAccount sa = accounts.get(principal);
    cCloud.deleteServiceAccount(sa.getId());
  }
}
