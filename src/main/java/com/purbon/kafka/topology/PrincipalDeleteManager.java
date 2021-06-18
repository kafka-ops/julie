package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.accounts.ClearAccounts;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrincipalDeleteManager extends AbstractPrincipalManager {

  public PrincipalDeleteManager(PrincipalProvider provider, Configuration config) {
    super(provider, config);
  }

  @Override
  protected void doUpdatePlan(
      ExecutionPlan plan,
      Topology topology,
      List<String> principals,
      Map<String, ServiceAccount> accounts)
      throws IOException {
    if (config.isAllowDeletePrincipals()) {
      provider.configure();
      // build list of principals to be deleted.
      List<ServiceAccount> principalsToBeDeleted =
          accounts.values().stream()
              .filter(currentPrincipal -> !principals.contains(currentPrincipal.getName()))
              .collect(Collectors.toList());
      if (!principalsToBeDeleted.isEmpty()) {
        plan.add(new ClearAccounts(provider, principalsToBeDeleted));
      }
    }
  }
}
