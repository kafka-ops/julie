package com.purbon.kafka.topology.serviceAccounts;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Set;

public class CCloudPrincipalProvider implements PrincipalProvider {

  private CCloudApi cCloudApi;
  private String env;

  public CCloudPrincipalProvider(Configuration config) {
    this.cCloudApi = new CCloudApi(config.getConfluentCloudClusterUrl(), config);
    this.env = config.getConfluentCloudEnv();
  }

  @Override
  public void configure() throws IOException {
    // NoOp
  }

  @Override
  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    return cCloudApi.listServiceAccounts();
  }

  @Override
  public ServiceAccount createServiceAccount(String principal, String description)
      throws IOException {
    return cCloudApi.createServiceAccount(principal, description);
  }

  @Override
  public void deleteServiceAccount(ServiceAccount serviceAccount) throws IOException {
    cCloudApi.deleteServiceAccount(serviceAccount.getId());
  }
}
