package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.*;
import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.accounts.ClearAccounts;
import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.actions.topics.CreateTopicAction;
import com.purbon.kafka.topology.actions.topics.DeleteTopics;
import com.purbon.kafka.topology.audit.Auditor;
import com.purbon.kafka.topology.audit.VoidAuditor;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.model.artefact.KsqlArtefact;
import com.purbon.kafka.topology.model.artefact.KsqlStreamArtefact;
import com.purbon.kafka.topology.model.artefact.KsqlTableArtefact;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.StreamUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecutionPlan {

  private static final Logger LOGGER = LogManager.getLogger(ExecutionPlan.class);

  private final List<Action> plan;
  private final PrintStream outputStream;
  private final BackendController backendController;

  private Set<TopologyAclBinding> bindings;
  private Set<ServiceAccount> serviceAccounts;
  private Set<String> topics;
  private Set<KafkaConnectArtefact> connectors;
  private Set<KsqlStreamArtefact> ksqlStreams;
  private Set<KsqlTableArtefact> ksqlTables;

  private Auditor auditor;

  private ExecutionPlan(
      List<Action> plan,
      PrintStream outputStream,
      BackendController backendController,
      Auditor auditor) {
    this.plan = plan;
    this.outputStream = outputStream;
    this.auditor = auditor;

    this.bindings = new HashSet<>();
    this.serviceAccounts = new HashSet<>();
    this.topics = new HashSet<>();
    this.connectors = new HashSet<>();
    this.ksqlStreams = new HashSet<>();
    this.ksqlTables = new HashSet<>();
    this.backendController = backendController;

    if (backendController.size() > 0) {
      this.bindings.addAll(backendController.getBindings());
      this.serviceAccounts.addAll(backendController.getServiceAccounts());
      this.topics.addAll(backendController.getTopics());
      this.connectors.addAll(backendController.getConnectors());
      this.ksqlStreams.addAll(backendController.getKSqlStreams());
      this.ksqlTables.addAll(backendController.getKSqlTables());
    }
  }

  public void add(Action action) {
    this.plan.add(action);
  }

  public static ExecutionPlan init(BackendController backendController, PrintStream outputStream)
      throws IOException {
    return init(backendController, outputStream, new VoidAuditor());
  }

  public static ExecutionPlan init(
      BackendController backendController, PrintStream outputStream, Auditor auditor)
      throws IOException {
    backendController.load();
    List<Action> listOfActions = Collections.synchronizedList(new LinkedList<>());
    return new ExecutionPlan(listOfActions, outputStream, backendController, auditor);
  }

  public void run() throws IOException {
    run(false);
  }

  public void run(boolean dryRun) throws IOException {
    for (Action action : plan) {
      try {
        execute(action, dryRun);
      } catch (IOException e) {
        LOGGER.error(String.format("Something happen running action %s", action), e);
        throw e;
      }
    }

    if (!dryRun) {
      backendController.reset();
      backendController.addBindings(new ArrayList<>(bindings));
      backendController.addServiceAccounts(serviceAccounts);
      backendController.addTopics(topics);
      backendController.addConnectors(connectors);
      backendController.addKSqlStreams(ksqlStreams);
      backendController.addKSqlTables(ksqlTables);
      backendController.flushAndClose();
    }
  }

  private void execute(Action action, boolean dryRun) throws IOException {
    LOGGER.debug(String.format("Execution action %s (dryRun=%s)", action, dryRun));
    if (!action.toString().isEmpty()) {
      outputStream.println(action);
    }
    if (!dryRun) {
      action.run();
      auditor.log(action);
      // TODO: a nicer and more clean version of this might be a cool thing to have, current version
      // is shitty.
      if (action instanceof CreateTopicAction) {
        topics.add(((CreateTopicAction) action).getTopic());
      } else if (action instanceof DeleteTopics) {
        List<String> topicsToBeDeleted = ((DeleteTopics) action).getTopicsToBeDeleted();
        topics =
            new StreamUtils<>(topics.stream())
                .filterAsSet(topic -> !topicsToBeDeleted.contains(topic));
      }
      if (action instanceof BaseAccessControlAction
          && !((BaseAccessControlAction) action).getAclBindings().isEmpty()) {
        if (action instanceof ClearBindings) {
          bindings =
              new StreamUtils<>(bindings.stream())
                  .filterAsSet(
                      binding ->
                          !((BaseAccessControlAction) action).getAclBindings().contains(binding));
        } else {
          bindings.addAll(((BaseAccessControlAction) action).getAclBindings());
        }
      }
      if (action instanceof BaseAccountsAction) {
        if (action instanceof ClearAccounts) {
          Collection<ServiceAccount> toDeletePrincipals = ((ClearAccounts) action).getPrincipals();
          serviceAccounts =
              new StreamUtils<>(serviceAccounts.stream())
                  .filterAsSet(sa -> !toDeletePrincipals.contains(sa));
        } else {
          CreateAccounts createAction = (CreateAccounts) action;
          serviceAccounts.addAll(createAction.getPrincipals());
        }
      }

      if (action instanceof CreateArtefactAction) {
        Artefact artefact = ((CreateArtefactAction) action).getArtefact();
        if (artefact instanceof KafkaConnectArtefact) {
          connectors.add((KafkaConnectArtefact) artefact);
        } else if (artefact instanceof KsqlStreamArtefact) {
          ksqlStreams.add((KsqlStreamArtefact) artefact);
        } else if (artefact instanceof KsqlTableArtefact) {
          ksqlTables.add((KsqlTableArtefact) artefact);
        }
      } else if (action instanceof SyncArtefactAction) {
        Artefact artefact = ((SyncArtefactAction) action).getArtefact();
        if (artefact instanceof KafkaConnectArtefact) {
          connectors =
              new StreamUtils<>(connectors.stream())
                  .filterAsSet(connector -> !connector.equals(artefact));
          connectors.add((KafkaConnectArtefact) artefact);
        }
      } else if (action instanceof DeleteArtefactAction) {
        Artefact toBeDeleted = ((DeleteArtefactAction) action).getArtefact();
        if (toBeDeleted instanceof KafkaConnectArtefact) {
          connectors =
              new StreamUtils<>(connectors.stream())
                  .filterAsSet(connector -> !connector.equals(toBeDeleted));
        } else if (toBeDeleted instanceof KsqlStreamArtefact) {
          ksqlStreams =
              new StreamUtils<>(ksqlStreams.stream())
                  .filterAsSet(ksql -> !ksql.equals(toBeDeleted));
        } else if (toBeDeleted instanceof KsqlTableArtefact) {
          ksqlTables =
              new StreamUtils<>(ksqlTables.stream()).filterAsSet(ksql -> !ksql.equals(toBeDeleted));
        }
      }
    }
  }

  public Set<ServiceAccount> getServiceAccounts() {
    return serviceAccounts;
  }

  public Set<TopologyAclBinding> getBindings() {
    return bindings;
  }

  public Set<String> getTopics() {
    return topics;
  }

  public List<Action> getActions() {
    return plan;
  }

  public Set<KafkaConnectArtefact> getConnectors() {
    return connectors;
  }

  public Set<? extends KsqlArtefact> getKSqlArtefacts() {
    return Stream.of(ksqlStreams, ksqlTables)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }
}
