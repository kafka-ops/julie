package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.Constants.MANAGED_BY;

import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrincipalUpdateManager extends AbstractPrincipalManager {

  private static final Logger LOGGER = LogManager.getLogger(PrincipalUpdateManager.class);

  public PrincipalUpdateManager(PrincipalProvider provider, Configuration config) {
    super(provider, config);
  }

  @Override
  protected void doUpdatePlan(
      ExecutionPlan plan,
      Topology topology,
      List<String> principals,
      Map<String, ServiceAccount> accounts) {
    LOGGER.debug(
        "Updating accounts for principals = "
            + principals.stream().collect(Collectors.joining(","))
            + " accounts = "
            + accounts.values().stream()
                .map(ServiceAccount::toString)
                .collect(Collectors.joining(", ")));
    // build set of principals to be created.
    Set<ServiceAccount> principalsToBeCreated =
        principals.stream()
            .filter(wishPrincipal -> !accounts.containsKey(wishPrincipal))
            .map(principal -> new ServiceAccount("-1", principal, MANAGED_BY))
            .collect(Collectors.toSet());

    if (!principalsToBeCreated.isEmpty()) {
      plan.add(new CreateAccounts(provider, principalsToBeCreated));
    }
  }
}
