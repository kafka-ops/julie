package com.purbon.kafka.topology.serviceAccounts;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class VoidPrincipalProvider implements PrincipalProvider {

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
