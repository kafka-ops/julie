package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Set;

public interface PrincipalProvider {

  void configure() throws IOException;

  Set<ServiceAccount> listServiceAccounts() throws IOException;

  ServiceAccount createServiceAccount(String principal, String description) throws IOException;

  void deleteServiceAccount(ServiceAccount serviceAccount) throws IOException;
}
