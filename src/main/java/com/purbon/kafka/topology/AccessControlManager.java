package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.model.Component.KAFKA;
import static com.purbon.kafka.topology.model.Component.KAFKA_CONNECT;
import static com.purbon.kafka.topology.model.Component.SCHEMA_REGISTRY;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.access.CreateBindings;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForConsumer;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForControlCenter;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForKConnect;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForKStreams;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForProducer;
import com.purbon.kafka.topology.actions.access.builders.BuildBindingsForSchemaRegistry;
import com.purbon.kafka.topology.actions.access.builders.rbac.BuildBindingsForConnectorAuthorization;
import com.purbon.kafka.topology.actions.access.builders.rbac.BuildBindingsForSchemaAuthorization;
import com.purbon.kafka.topology.actions.access.builders.rbac.BuildClusterLevelBinding;
import com.purbon.kafka.topology.actions.access.builders.rbac.BuildPredefinedBinding;
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
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);

  private final TopologyBuilderConfig config;
  private AccessControlProvider controlProvider;
  private BindingsBuilderProvider bindingsBuilder;

  public AccessControlManager(
      AccessControlProvider controlProvider, BindingsBuilderProvider builderProvider) {
    this(controlProvider, builderProvider, new TopologyBuilderConfig());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      BindingsBuilderProvider builderProvider,
      TopologyBuilderConfig config) {
    this.controlProvider = controlProvider;
    this.bindingsBuilder = builderProvider;
    this.config = config;
  }

  public void apply(final Topology topology, ExecutionPlan plan) throws IOException {

    List<Action> actions = new ArrayList<>();

    for (Project project : topology.getProjects()) {
      project
          .getTopics()
          .forEach(
              topic -> {
                final String fullTopicName = topic.toString();
                if (!project.getConsumers().isEmpty()) {
                  Action action =
                      new BuildBindingsForConsumer(
                          bindingsBuilder, project.getConsumers(), fullTopicName);
                  actions.add(action);
                }
                if (!project.getProducers().isEmpty()) {
                  Action action =
                      new BuildBindingsForProducer(
                          bindingsBuilder, project.getProducers(), fullTopicName);
                  actions.add(action);
                }
              });
      // Setup global Kafka Stream Access control lists
      String topicPrefix = project.namePrefix();
      for (KStream app : project.getStreams()) {
        Action action = syncApplicationAcls(app, topicPrefix);
        actions.add(action);
      }
      for (Connector connector : project.getConnectors()) {
        Action action = syncApplicationAcls(connector, topicPrefix);
        actions.add(action);
        if (connector.getConnectors().isPresent())
          actions.add(new BuildBindingsForConnectorAuthorization(bindingsBuilder, connector));
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        actions.add(new BuildBindingsForSchemaAuthorization(bindingsBuilder, schemaAuthorization));
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix, actions);
    }

    syncPlatformAcls(topology, actions);

    // Main actions now should be setup to create low level bindings

    Set<TopologyAclBinding> allFinalBindings =
        actions.stream().flatMap(executeToFunction()).collect(Collectors.toSet());

    // Diff of bindings, so we only create what is not already created in the cluster.
    Set<TopologyAclBinding> bindingsToBeCreated =
        allFinalBindings.stream()
            .filter(binding -> !plan.getBindings().contains(binding))
            .collect(Collectors.toSet());

    CreateBindings createBindings = new CreateBindings(controlProvider, bindingsToBeCreated);
    plan.add(createBindings);

    if (config.allowDelete()) {
      // clear acls that does not appear anymore in the new generated list,
      // but where previously created
      Set<TopologyAclBinding> bindingsToDelete =
          plan.getBindings().stream()
              .filter(binding -> !allFinalBindings.contains(binding))
              .collect(Collectors.toSet());

      ClearBindings clearBindings = new ClearBindings(controlProvider, bindingsToDelete);
      plan.add(clearBindings);
    }
  }

  private Function<Action, Stream<TopologyAclBinding>> executeToFunction() {
    return action -> {
      try {
        action.run();
        return action.getBindings().stream();
      } catch (Exception ex) {
        LOGGER.error(ex);
        return new ArrayList<TopologyAclBinding>().stream();
      }
    };
  }

  private void syncPlatformAcls(final Topology topology, List<Action> actions) throws IOException {
    // Sync platform relevant Access Control List.
    Platform platform = topology.getPlatform();

    // Set cluster level ACLs
    syncClusterLevelRbac(platform.getKafka().getRbac(), KAFKA, actions);
    syncClusterLevelRbac(platform.getKafkaConnect().getRbac(), KAFKA_CONNECT, actions);
    syncClusterLevelRbac(platform.getSchemaRegistry().getRbac(), SCHEMA_REGISTRY, actions);

    // Set component level ACLs
    for (SchemaRegistryInstance schemaRegistry : platform.getSchemaRegistry().getInstances()) {
      actions.add(new BuildBindingsForSchemaRegistry(bindingsBuilder, schemaRegistry));
    }
    for (ControlCenterInstance controlCenter : platform.getControlCenter().getInstances()) {
      actions.add(new BuildBindingsForControlCenter(bindingsBuilder, controlCenter));
    }
  }

  private void syncClusterLevelRbac(
      Optional<Map<String, List<User>>> rbac, Component cmp, List<Action> actions) {
    if (rbac.isPresent()) {
      Map<String, List<User>> roles = rbac.get();
      for (String role : roles.keySet()) {
        for (User user : roles.get(role)) {
          actions.add(new BuildClusterLevelBinding(bindingsBuilder, role, user, cmp));
        }
      }
    }
  }

  private void syncRbacRawRoles(
      Map<String, List<String>> rbacRawRoles, String topicPrefix, List<Action> actions) {
    rbacRawRoles.forEach(
        (predefinedRole, principals) ->
            principals.forEach(
                principal ->
                    actions.add(
                        new BuildPredefinedBinding(
                            bindingsBuilder, principal, predefinedRole, topicPrefix))));
  }

  private Action syncApplicationAcls(DynamicUser app, String topicPrefix) throws IOException {
    if (app instanceof KStream) {
      return new BuildBindingsForKStreams(bindingsBuilder, (KStream) app, topicPrefix);
    } else if (app instanceof Connector) {
      return new BuildBindingsForKConnect(bindingsBuilder, (Connector) app, topicPrefix);
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

  public void setAclsProvider(SimpleAclsProvider aclsProvider) {
    this.controlProvider = aclsProvider;
  }
}
