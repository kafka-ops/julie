package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PrincipalUpdateManager extends AbstractPrincipalManager {

  public PrincipalUpdateManager(PrincipalProvider provider, Configuration config) {
    super(provider, config);
  }

  @Override
  protected void doUpdatePlan(
      ExecutionPlan plan,
      Topology topology,
      List<String> principals,
      Map<String, ServiceAccount> accounts) {
    // build set of principals to be created.
    Set<ServiceAccount> principalsToBeCreated =
        principals.stream()
            .filter(wishPrincipal -> !accounts.containsKey(wishPrincipal))
            .map(principal -> new ServiceAccount(-1, principal, "Managed by KTB"))
            .collect(Collectors.toSet());

    if (!principalsToBeCreated.isEmpty()) {
      plan.add(new CreateAccounts(provider, principalsToBeCreated));
    }
  }
}
