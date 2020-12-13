package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.accounts.ClearAccounts;
import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrincipalManager {

  private static final Logger LOGGER = LogManager.getLogger(PrincipalManager.class);

  private PrincipalProvider provider;

  private TopologyBuilderConfig config;

  public PrincipalManager(PrincipalProvider provider, TopologyBuilderConfig config) {
    this.provider = provider;
    this.config = config;
  }

  public void apply(Topology topology, ExecutionPlan plan) throws IOException {
    if (!config.enabledExperimental()) {
      LOGGER.debug("Not running the PrincipalsManager as this is an experimental feature.");
      return;
    }
    // Do Nothing if the provider is the void one.
    // This means the management of principals is either not possible or has not been configured
    List<String> principals = parseListOfPrincipals(topology);
    if (!(provider instanceof VoidPrincipalProvider)) {
      provider.configure();
      managePrincipals(principals, plan);
    }
  }

  private void managePrincipals(List<String> principals, ExecutionPlan plan) {

    Map<String, ServiceAccount> accounts =
        plan.getServiceAccounts().stream()
            .collect(Collectors.toMap(ServiceAccount::getName, serviceAccount -> serviceAccount));

    // build list of principals to be created.
    List<ServiceAccount> principalsToBeCreated =
        principals.stream()
            .filter(wishPrincipal -> !accounts.containsKey(wishPrincipal))
            .map(principal -> new ServiceAccount(-1, principal, "Managed by KTB"))
            .collect(Collectors.toList());

    if (!principalsToBeCreated.isEmpty()) {
      plan.add(new CreateAccounts(provider, principalsToBeCreated));
    }

    // build list of principals to be deleted.
    if (config.allowDelete() || config.isAllowDeletePrincipals()) {
      List<ServiceAccount> principalsToBeDeleted =
          accounts.values().stream()
              .filter(currentPrincipal -> !principals.contains(currentPrincipal.getName()))
              .collect(Collectors.toList());
      if (!principalsToBeDeleted.isEmpty()) {
        plan.add(new ClearAccounts(provider, principalsToBeDeleted));
      }
    }
  }

  private List<String> parseListOfPrincipals(Topology topology) {
    return topology.getProjects().stream()
        .flatMap(
            project -> {
              List<User> users = new ArrayList<>();
              users.addAll(project.getConsumers());
              users.addAll(project.getProducers());
              users.addAll(project.getStreams());
              users.addAll(project.getConnectors());
              users.addAll(project.getSchemas());
              return users.stream();
            })
        .map(User::getPrincipal)
        .collect(Collectors.toList());
  }
}
