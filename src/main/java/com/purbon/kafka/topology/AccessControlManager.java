package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.DRY_RUN_OPTION;
import static com.purbon.kafka.topology.model.Component.KAFKA;
import static com.purbon.kafka.topology.model.Component.KAFKA_CONNECT;
import static com.purbon.kafka.topology.model.Component.SCHEMA_REGISTRY;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.AddConnectorAuthorization;
import com.purbon.kafka.topology.actions.ClearAcls;
import com.purbon.kafka.topology.actions.SetAclsForConsumer;
import com.purbon.kafka.topology.actions.SetAclsForControlCenter;
import com.purbon.kafka.topology.actions.SetAclsForKConnect;
import com.purbon.kafka.topology.actions.SetAclsForKStreams;
import com.purbon.kafka.topology.actions.SetAclsForProducer;
import com.purbon.kafka.topology.actions.SetAclsForSchemaRegistry;
import com.purbon.kafka.topology.actions.SetClusterLevelRole;
import com.purbon.kafka.topology.actions.SetPredefinedRole;
import com.purbon.kafka.topology.actions.SetSchemaAuthorization;
import com.purbon.kafka.topology.model.Component;
import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);
  private final Boolean allowDelete;
  private final List<Action> plan;
  private boolean dryRun;
  private PrintStream outputStream;

  private AccessControlProvider controlProvider;
  private ClusterState clusterState;
  private Map<String, String> cliParams;

  public AccessControlManager(AccessControlProvider controlProvider) {
    this(controlProvider, new ClusterState(), new HashMap<>());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider, Map<String, String> cliParams) {
    this(controlProvider, new ClusterState(), cliParams);
  }

  public AccessControlManager(AccessControlProvider controlProvider, ClusterState clusterState) {
    this(controlProvider, clusterState, new HashMap<>());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      ClusterState clusterState,
      Map<String, String> cliParams) {
    this.controlProvider = controlProvider;
    this.clusterState = clusterState;
    this.cliParams = cliParams;
    this.plan = new ArrayList<>();
    this.allowDelete = Boolean.valueOf(cliParams.getOrDefault(ALLOW_DELETE_OPTION, "true"));
    this.dryRun = Boolean.valueOf(cliParams.get(DRY_RUN_OPTION));
    this.outputStream = System.out;
  }

  public void clearAcls() {
    try {
      clusterState.load();
      if (allowDelete) {
        plan.add(new ClearAcls(controlProvider, clusterState));
      }
    } catch (Exception e) {
      LOGGER.error(e);
    } finally {
      if (allowDelete && !dryRun) {
        clusterState.reset();
      }
    }
  }

  public void sync(final Topology topology) throws IOException {
    plan.clear();
    clearAcls();

    for (Project project : topology.getProjects()) {
      project
          .getTopics()
          .forEach(
              topic -> {
                final String fullTopicName = topic.toString();

                project.getConsumers().stream()
                    .map(
                        (Function<Consumer, Action>)
                            consumer ->
                                new SetAclsForConsumer(controlProvider, consumer, fullTopicName))
                    .forEachOrdered(action -> plan.add(action));

                project.getProducers().stream()
                    .map(
                        (Function<Producer, Action>)
                            producer ->
                                new SetAclsForProducer(controlProvider, producer, fullTopicName))
                    .forEachOrdered(action -> plan.add(action));
              });
      // Setup global Kafka Stream Access control lists
      String topicPrefix = project.buildTopicPrefix(topology.buildNamePrefix());
      for (KStream app : project.getStreams()) {
        Action action = syncApplicationAcls(app, topicPrefix);
        plan.add(action);
      }
      for (Connector connector : project.getConnectors()) {
        Action action = syncApplicationAcls(connector, topicPrefix);
        plan.add(action);
        if (connector.getConnectors().isPresent())
          plan.add(new AddConnectorAuthorization(controlProvider, connector));
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        plan.add(new SetSchemaAuthorization(controlProvider, schemaAuthorization));
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix);
    }

    syncPlatformAcls(topology);

    for (Action action : plan) {
      if (dryRun) {
        outputStream.println(action);
      } else {
        action.run();
        clusterState.add(action.getBindings());
      }
    }
    clusterState.flushAndClose();
  }

  private void syncPlatformAcls(final Topology topology) throws IOException {
    // Sync platform relevant Access Control List.
    Platform platform = topology.getPlatform();

    // Set cluster level ACLs
    syncClusterLevelRbac(platform.getKafka().getRbac(), KAFKA);
    syncClusterLevelRbac(platform.getKafkaConnect().getRbac(), KAFKA_CONNECT);
    syncClusterLevelRbac(platform.getSchemaRegistry().getRbac(), SCHEMA_REGISTRY);

    // Set component level ACLs
    for (SchemaRegistryInstance schemaRegistry : platform.getSchemaRegistry().getInstances()) {
      plan.add(new SetAclsForSchemaRegistry(controlProvider, schemaRegistry));
    }
    for (ControlCenterInstance controlCenter : platform.getControlCenter().getInstances()) {
      plan.add(new SetAclsForControlCenter(controlProvider, controlCenter));
    }
  }

  private void syncClusterLevelRbac(Optional<Map<String, List<User>>> rbac, Component cmp) {
    if (rbac.isPresent()) {
      Map<String, List<User>> roles = rbac.get();
      for (String role : roles.keySet()) {
        for (User user : roles.get(role)) {
          plan.add(new SetClusterLevelRole(controlProvider, role, user, cmp));
        }
      }
    }
  }

  private void syncRbacRawRoles(Map<String, List<String>> rbacRawRoles, String topicPrefix) {
    rbacRawRoles.forEach(
        (predefinedRole, principals) ->
            principals.forEach(
                principal ->
                    plan.add(
                        new SetPredefinedRole(
                            controlProvider, principal, predefinedRole, topicPrefix))));
  }

  private Action syncApplicationAcls(DynamicUser app, String topicPrefix) throws IOException {
    if (app instanceof KStream) {
      return new SetAclsForKStreams(controlProvider, (KStream) app, topicPrefix);
    } else if (app instanceof Connector) {
      return new SetAclsForKConnect(controlProvider, (Connector) app, topicPrefix);
    } else {
      throw new IOException("Wrong dynamic app used.");
    }
  }

  public void printCurrentState(PrintStream out) {
    out.println("List of ACLs: ");
    controlProvider
        .listAcls()
        .forEach(
            (topic, aclBindings) -> {
              out.println(topic);
              aclBindings.forEach(binding -> out.println(binding));
            });
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public void setOutputStream(PrintStream os) {
    this.outputStream = os;
  }
}
