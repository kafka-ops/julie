package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.model.Component.KAFKA;
import static com.purbon.kafka.topology.model.Component.KAFKA_CONNECT;
import static com.purbon.kafka.topology.model.Component.SCHEMA_REGISTRY;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.AddConnectorAuthorization;
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
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);

  private final Boolean allowDelete;
  private ExecutionPlan plan;
  private boolean dryRun;
  private PrintStream outputStream;

  private AccessControlProvider controlProvider;
  private ClusterState clusterState;

  public AccessControlManager(AccessControlProvider controlProvider) {
    this(controlProvider, new ClusterState(), new TopologyBuilderConfig());
  }

  public AccessControlManager(AccessControlProvider controlProvider, TopologyBuilderConfig config) {
    this(controlProvider, new ClusterState(), config);
  }

  public AccessControlManager(AccessControlProvider controlProvider, ClusterState clusterState) {
    this(controlProvider, clusterState, new TopologyBuilderConfig());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      ClusterState clusterState,
      TopologyBuilderConfig config) {
    this.controlProvider = controlProvider;
    this.clusterState = clusterState;
    this.plan = new ExecutionPlan();
    this.allowDelete = config.allowDeletes();
    this.dryRun = config.isDryRun();
    this.outputStream = System.out;
  }

  public void sync(final Topology topology) throws IOException {
    plan.init(clusterState, allowDelete, outputStream);

    for (Project project : topology.getProjects()) {
      project
          .getTopics()
          .forEach(
              topic -> {
                final String fullTopicName = topic.toString();
                if (!project.getConsumers().isEmpty()) {
                  Action action =
                      new SetAclsForConsumer(
                          controlProvider, project.getConsumers(), fullTopicName);
                  plan.add(action);
                }
                if (!project.getProducers().isEmpty()) {
                  Action action =
                      new SetAclsForProducer(
                          controlProvider, project.getProducers(), fullTopicName);
                  plan.add(action);
                }
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

    plan.run(dryRun, controlProvider);
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

  public void setAclsProvider(SimpleAclsProvider aclsProvider) {
    this.controlProvider = aclsProvider;
  }
}
