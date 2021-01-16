package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.model.Component.KAFKA;
import static com.purbon.kafka.topology.model.Component.KAFKA_CONNECT;
import static com.purbon.kafka.topology.model.Component.SCHEMA_REGISTRY;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.access.CreateBindings;
import com.purbon.kafka.topology.actions.access.builders.*;
import com.purbon.kafka.topology.actions.access.builders.rbac.*;
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
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.PrintStream;
import java.util.*;
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
  private final List<String> managedServiceAccountPrefixes;
  private final List<String> managedTopicPrefixes;
  private final List<String> managedGroupPrefixes;

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
    this.managedServiceAccountPrefixes = config.getServiceAccountManagedPrefixes();
    this.managedTopicPrefixes = config.getTopicManagedPrefixes();
    this.managedGroupPrefixes = config.getGroupManagedPrefixes();
  }

  /**
   * Main apply method, append to the execution plan the necessary bindings to update the access
   * control
   *
   * @param topology A topology file descriptor
   * @param plan An Execution plan
   */
  public void apply(final Topology topology, ExecutionPlan plan) {
    List<Action> actions = buildProjectActions(topology);
    actions.addAll(buildPlatformLevelActions(topology));
    buildUpdateBindingsActions(actions, loadActualClusterStateIfAvailable(plan)).forEach(plan::add);
  }

  private Set<TopologyAclBinding> loadActualClusterStateIfAvailable(ExecutionPlan plan) {
    Set<TopologyAclBinding> bindings =
        config.fetchStateFromTheCluster() ? providerBindings() : plan.getBindings();
    return bindings.stream().filter(this::matchesManagedPrefixList).collect(Collectors.toSet());
  }

  private Set<TopologyAclBinding> providerBindings() {
    Set<TopologyAclBinding> bindings = new HashSet<>();
    controlProvider.listAcls().values().forEach(bindings::addAll);
    return bindings;
  }

  /**
   * Build the core list of actions builders for creating access control rules
   *
   * @param topology A topology file
   * @return List<Action> A list of actions required based on the parameters
   */
  public List<Action> buildProjectActions(Topology topology) {
    List<Action> actions = new ArrayList<>();

    for (Project project : topology.getProjects()) {
      if (config.shouldOptimizeAcls()) {
        actions.addAll(buildOptimizeConsumerAndProducerAcls(project));
      } else {
        actions.addAll(buildDetailedConsumerAndProducerAcls(project));
      }
      // Setup global Kafka Stream Access control lists
      String topicPrefix = project.namePrefix();
      for (KStream app : project.getStreams()) {
        syncApplicationAcls(app, topicPrefix).ifPresent(actions::add);
      }
      for (Connector connector : project.getConnectors()) {
        syncApplicationAcls(connector, topicPrefix).ifPresent(actions::add);
        connector
            .getConnectors()
            .ifPresent(
                (list) -> {
                  actions.add(
                      new BuildBindingsForConnectorAuthorization(bindingsBuilder, connector));
                });
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        actions.add(new BuildBindingsForSchemaAuthorization(bindingsBuilder, schemaAuthorization));
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix, actions);
    }
    return actions;
  }

  private List<Action> buildOptimizeConsumerAndProducerAcls(Project project) {
    List<Action> actions = new ArrayList<>();
    actions.add(
        new BuildBindingsForConsumer(
            bindingsBuilder, project.getConsumers(), project.namePrefix(), true));
    actions.add(
        new BuildBindingsForProducer(
            bindingsBuilder, project.getProducers(), project.namePrefix(), true));

    // When optimised, still need to add any topic level specific.
    actions.addAll(buildBasicUsersAcls(project, false));
    return actions;
  }

  private List<Action> buildDetailedConsumerAndProducerAcls(Project project) {
    return buildBasicUsersAcls(project, true);
  }

  private List<Action> buildBasicUsersAcls(Project project, boolean includeProjectLevel) {
    List<Action> actions = new ArrayList<>();
    project
        .getTopics()
        .forEach(
            topic -> {
              final String fullTopicName = topic.toString();
              Set<Consumer> consumers = new HashSet(topic.getConsumers());
              if (includeProjectLevel) {
                consumers.addAll(project.getConsumers());
              }
              if (!consumers.isEmpty()) {
                Action action =
                    new BuildBindingsForConsumer(
                        bindingsBuilder, new ArrayList<>(consumers), fullTopicName, false);
                actions.add(action);
              }
              Set<Producer> producers = new HashSet(topic.getProducers());
              if (includeProjectLevel) {
                producers.addAll(project.getProducers());
              }
              if (!producers.isEmpty()) {
                Action action =
                    new BuildBindingsForProducer(
                        bindingsBuilder, new ArrayList<>(producers), fullTopicName, false);
                actions.add(action);
              }
            });
    return actions;
  }

  /**
   * Build a list of actions required to create or delete necessary bindings
   *
   * @param actions List of pre computed actions based on a topology
   * @param bindings List of current bindings available in the cluster
   * @return List<Action> list of actions necessary to update the cluster
   */
  public List<Action> buildUpdateBindingsActions(
      List<Action> actions, Set<TopologyAclBinding> bindings) {

    List<Action> updateActions = new ArrayList<>();

    Set<TopologyAclBinding> allFinalBindings =
        actions.stream().flatMap(actionApplyFunction()).collect(Collectors.toSet());

    Set<TopologyAclBinding> bindingsToBeCreated =
        allFinalBindings.stream()
            .filter(Objects::nonNull)
            // Only create what we manage
            .filter(this::matchesManagedPrefixList)
            // Diff of bindings, so we only create what is not already created in the cluster.
            .filter(binding -> !bindings.contains(binding))
            .collect(Collectors.toSet());

    if (!bindingsToBeCreated.isEmpty()) {
      CreateBindings createBindings = new CreateBindings(controlProvider, bindingsToBeCreated);
      updateActions.add(createBindings);
    }

    if (config.allowDelete() || config.isAllowDeleteBindings()) {
      // clear acls that does not appear anymore in the new generated list,
      // but where previously created
      Set<TopologyAclBinding> bindingsToDelete =
          bindings.stream()
              .filter(binding -> !allFinalBindings.contains(binding))
              .collect(Collectors.toSet());
      if (!bindingsToDelete.isEmpty()) {
        ClearBindings clearBindings = new ClearBindings(controlProvider, bindingsToDelete);
        updateActions.add(clearBindings);
      }
    }
    return updateActions;
  }

  private boolean matchesManagedPrefixList(TopologyAclBinding topologyAclBinding) {
    String resourceName = topologyAclBinding.getResourceName();
    String principle = topologyAclBinding.getPrincipal();
    // For global wild cards ACL's we manage only if we manage the service account/principle,
    // regardless.
    if (resourceName.equals("*")) {
      return matchesServiceAccountPrefixList(principle);
    }

    switch (topologyAclBinding.getResourceType()) {
      case TOPIC:
        return matchesTopicPrefixList(resourceName);
      case GROUP:
        return matchesGroupPrefixList(resourceName);
      default:
        return matchesServiceAccountPrefixList(principle);
    }
  }

  private boolean matchesTopicPrefixList(String topic) {
    boolean matches =
        managedTopicPrefixes.size() == 0
            || managedTopicPrefixes.stream().anyMatch(topic::startsWith);
    LOGGER.debug(
        String.format("Topic %s matches %s with $s", topic, matches, managedTopicPrefixes));
    return matches;
  }

  private boolean matchesGroupPrefixList(String group) {
    boolean matches =
        managedGroupPrefixes.size() == 0
            || managedGroupPrefixes.stream().anyMatch(group::startsWith);
    LOGGER.debug(
        String.format("Group %s matches %s with $s", group, matches, managedGroupPrefixes));
    return matches;
  }

  private boolean matchesServiceAccountPrefixList(String principal) {
    boolean matches =
        managedServiceAccountPrefixes.size() == 0
            || managedServiceAccountPrefixes.stream().anyMatch(principal::startsWith);
    LOGGER.debug(
        String.format(
            "Principal %s matches %s with $s", principal, matches, managedServiceAccountPrefixes));
    return matches;
  }

  private Function<Action, Stream<TopologyAclBinding>> actionApplyFunction() {
    return action -> {
      try {
        action.run();
        return action.getBindings().stream();
      } catch (Exception ex) {
        LOGGER.error(ex);
        return Stream.empty();
      }
    };
  }

  // Sync platform relevant Access Control List.
  public List<Action> buildPlatformLevelActions(final Topology topology) {
    List<Action> actions = new ArrayList<>();
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
    return actions;
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

  private Optional<Action> syncApplicationAcls(DynamicUser app, String topicPrefix) {
    Action action = null;
    if (app instanceof KStream) {
      action = new BuildBindingsForKStreams(bindingsBuilder, (KStream) app, topicPrefix);
    } else if (app instanceof Connector) {
      action = new BuildBindingsForKConnect(bindingsBuilder, (Connector) app, topicPrefix);
    }
    return Optional.ofNullable(action);
  }

  public void printCurrentState(PrintStream out) {
    out.println("List of ACLs: ");
    controlProvider
        .listAcls()
        .forEach(
            (topic, aclBindings) -> {
              out.println(topic);
              aclBindings.forEach(out::println);
            });
  }
}
