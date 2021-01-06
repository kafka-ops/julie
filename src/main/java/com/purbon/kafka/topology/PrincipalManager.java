package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.accounts.ClearAccounts;
import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrincipalManager {

  private static final Logger LOGGER = LogManager.getLogger(PrincipalManager.class);
  private final List<String> managedPrefixes;

  private PrincipalProvider provider;

  private TopologyBuilderConfig config;

  public PrincipalManager(PrincipalProvider provider, TopologyBuilderConfig config) {
    this.provider = provider;
    this.config = config;
    this.managedPrefixes = config.getServiceAccountManagedPrefixes();
  }

  public void applyCreate(Topology topology, ExecutionPlan plan) throws IOException {
    if (!config.enabledExperimental()) {
      LOGGER.debug("Not running the PrincipalsManager as this is an experimental feature.");
      return;
    }
    if (provider instanceof VoidPrincipalProvider) {
      // Do Nothing if the provider is the void one.
      // This means the management of principals is either not possible or has not been configured
      return;
    }

    provider.configure();

    List<String> principals = parseListOfPrincipals(topology);
    Map<String, ServiceAccount> accounts = loadActualClusterStateIfAvailable(plan);

    // build list of principals to be created.
    List<ServiceAccount> principalsToBeCreated =
        principals.stream()
            .filter(wishPrincipal -> !accounts.containsKey(wishPrincipal))
            .map(principal -> new ServiceAccount(-1, principal, "Managed by KTB"))
            .collect(Collectors.toList());

    if (!principalsToBeCreated.isEmpty()) {
      plan.add(new CreateAccounts(provider, principalsToBeCreated));
    }
  }

  public void applyDelete(Topology topology, ExecutionPlan plan) throws IOException {
    if (!config.enabledExperimental()) {
      LOGGER.debug("Not running the PrincipalsManager as this is an experimental feature.");
      return;
    }
    if (provider instanceof VoidPrincipalProvider) {
      // Do Nothing if the provider is the void one.
      // This means the management of principals is either not possible or has not been configured
      return;
    }

    if (config.allowDelete() || config.isAllowDeletePrincipals()) {
      provider.configure();

      List<String> principals = parseListOfPrincipals(topology);
      Map<String, ServiceAccount> accounts = loadActualClusterStateIfAvailable(plan);

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

  private Map<String, ServiceAccount> loadActualClusterStateIfAvailable(ExecutionPlan plan)
      throws IOException {
    Set<ServiceAccount> accounts =
        config.fetchStateFromTheCluster()
            ? provider.listServiceAccounts()
            : plan.getServiceAccounts();
    return accounts.stream()
        .filter(serviceAccount -> matchesPrefixList(serviceAccount.getName()))
        .collect(Collectors.toMap(ServiceAccount::getName, serviceAccount -> serviceAccount));
  }

  private boolean matchesPrefixList(String principal) {
    boolean matches =
        managedPrefixes.size() == 0 || managedPrefixes.stream().anyMatch(principal::startsWith);
    LOGGER.debug(
        String.format("Principal %s matches %s with $s", principal, matches, managedPrefixes));
    return matches;
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
              for (Topic topic : project.getTopics()) {
                users.addAll(topic.getConsumers());
                users.addAll(topic.getProducers());
              }
              return users.stream();
            })
        .map(User::getPrincipal)
        .filter(this::matchesPrefixList)
        .collect(Collectors.toList());
  }

  public void printCurrentState(PrintStream out) throws IOException {
    out.println("List of Principles: ");
    provider.listServiceAccounts().forEach(out::println);
  }
}
