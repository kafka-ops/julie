package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.model.Component.*;

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
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);

  private final Configuration config;
  private AccessControlProvider controlProvider;
  private BindingsBuilderProvider bindingsBuilder;
  private final List<String> managedServiceAccountPrefixes;
  private final List<String> managedTopicPrefixes;
  private final List<String> managedGroupPrefixes;

  public AccessControlManager(
      AccessControlProvider controlProvider, BindingsBuilderProvider builderProvider) {
    this(controlProvider, builderProvider, new Configuration());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      BindingsBuilderProvider builderProvider,
      Configuration config) {
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
   * @param plan An Execution plan
   * @param topology A topology file descriptor
   */
  @Override
  public void updatePlan(ExecutionPlan plan, final Topology topology) throws IOException {
    List<AclBindingsResult> aclBindingsResults = buildProjectAclBindings(topology);
    aclBindingsResults.addAll(buildPlatformLevelActions(topology));
    buildUpdateBindingsActions(aclBindingsResults, loadActualClusterStateIfAvailable(plan))
        .forEach(plan::add);
  }

  private Set<TopologyAclBinding> loadActualClusterStateIfAvailable(ExecutionPlan plan) {
    Set<TopologyAclBinding> bindings =
        config.fetchStateFromTheCluster() ? providerBindings() : plan.getBindings();
    return bindings.stream()
        .filter(this::matchesManagedPrefixList)
        .filter(this::isNotInternalAcl)
        .collect(Collectors.toSet());
  }

  private boolean isNotInternalAcl(TopologyAclBinding binding) {
    Optional<String> internalPrincipal = config.getInternalPrincipalOptional();
    return internalPrincipal.map(i -> !binding.getPrincipal().equals(i)).orElse(true);
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
  private List<AclBindingsResult> buildProjectAclBindings(Topology topology) {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();

    for (Project project : topology.getProjects()) {
      if (config.shouldOptimizeAcls()) {
        aclBindingsResults.addAll(buildOptimizeConsumerAndProducerAcls(project));
      } else {
        aclBindingsResults.addAll(buildDetailedConsumerAndProducerAcls(project));
      }
      // Setup global Kafka Stream Access control lists
      String topicPrefix = project.namePrefix();
      for (KStream app : project.getStreams()) {
        syncApplicationAcls(app, topicPrefix).ifPresent(aclBindingsResults::add);
      }
      for (KSqlApp kSqlApp : project.getKSqls()) {
        syncApplicationAcls(kSqlApp, topicPrefix).ifPresent(aclBindingsResults::add);
      }
      for (Connector connector : project.getConnectors()) {
        syncApplicationAcls(connector, topicPrefix).ifPresent(aclBindingsResults::add);
        connector
            .getConnectors()
            .ifPresent(
                (list) -> {
                  aclBindingsResults.add(
                      new ConnectorAuthorizationAclBindingsBuilder(bindingsBuilder, connector)
                          .getAclBindings());
                });
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        aclBindingsResults.add(
            new SchemaAuthorizationAclBindingsBuilder(bindingsBuilder, schemaAuthorization)
                .getAclBindings());
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix, aclBindingsResults);
    }
    return aclBindingsResults;
  }

  private List<AclBindingsResult> buildOptimizeConsumerAndProducerAcls(Project project) {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();
    aclBindingsResults.add(
        new ConsumerAclBindingsBuilder(
                bindingsBuilder, project.getConsumers(), project.namePrefix(), true)
            .getAclBindings());
    aclBindingsResults.add(
        new ProducerAclBindingsBuilder(
                bindingsBuilder, project.getProducers(), project.namePrefix(), true)
            .getAclBindings());

    // When optimised, still need to add any topic level specific.
    aclBindingsResults.addAll(buildBasicUsersAcls(project, false));
    return aclBindingsResults;
  }

  private List<AclBindingsResult> buildDetailedConsumerAndProducerAcls(Project project) {
    return buildBasicUsersAcls(project, true);
  }

  private List<AclBindingsResult> buildBasicUsersAcls(
      Project project, boolean includeProjectLevel) {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();
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
                AclBindingsResult aclBindingsResult =
                    new ConsumerAclBindingsBuilder(
                            bindingsBuilder, new ArrayList<>(consumers), fullTopicName, false)
                        .getAclBindings();
                aclBindingsResults.add(aclBindingsResult);
              }
              Set<Producer> producers = new HashSet(topic.getProducers());
              if (includeProjectLevel) {
                producers.addAll(project.getProducers());
              }
              if (!producers.isEmpty()) {
                AclBindingsResult aclBindingsResult =
                    new ProducerAclBindingsBuilder(
                            bindingsBuilder, new ArrayList<>(producers), fullTopicName, false)
                        .getAclBindings();
                aclBindingsResults.add(aclBindingsResult);
              }
            });
    return aclBindingsResults;
  }

  /**
   * Build a list of actions required to create or delete necessary bindings
   *
   * @param aclBindingsResults List of pre computed actions based on a topology
   * @param bindings List of current bindings available in the cluster
   * @return List<Action> list of actions necessary to update the cluster
   */
  private List<Action> buildUpdateBindingsActions(
      List<AclBindingsResult> aclBindingsResults, Set<TopologyAclBinding> bindings)
      throws IOException {

    List<Action> updateActions = new ArrayList<>();

    final List<String> errorMessages =
        aclBindingsResults.stream()
            .filter(AclBindingsResult::isError)
            .map(AclBindingsResult::getErrorMessage)
            .collect(Collectors.toList());
    if (!errorMessages.isEmpty()) {
      for (String errorMessage : errorMessages) {
        LOGGER.error(errorMessage);
      }
      throw new IOException(errorMessages.get(0));
    }

    Set<TopologyAclBinding> allFinalBindings =
        aclBindingsResults.stream()
            .flatMap(aboe -> aboe.getAclBindings().stream())
            .collect(Collectors.toSet());

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

    if (config.isAllowDeleteBindings()) {
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
    // For global wild cards ACLs we manage only if we manage the service account/principle,
    // regardless.
    if (resourceName.equals("*")) {
      return matchesServiceAccountPrefixList(principle);
    }

    if ("TOPIC".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
      return matchesTopicPrefixList(resourceName);
    } else if ("GROUP".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
      return matchesGroupPrefixList(resourceName);
    } else {
      return matchesServiceAccountPrefixList(principle);
    }
  }

  private boolean matchesTopicPrefixList(String topic) {
    return matchesPrefix(managedTopicPrefixes, topic, "Topic");
  }

  private boolean matchesGroupPrefixList(String group) {
    return matchesPrefix(managedGroupPrefixes, group, "Group");
  }

  private boolean matchesServiceAccountPrefixList(String principal) {
    return matchesPrefix(managedServiceAccountPrefixes, principal, "Principal");
  }

  private boolean matchesPrefix(List<String> prefixes, String item, String type) {
    boolean matches = prefixes.size() == 0 || prefixes.stream().anyMatch(item::startsWith);
    LOGGER.debug(String.format("%s %s matches %s with $s", type, item, matches, prefixes));
    return matches;
  }

  // Sync platform relevant Access Control List.
  private List<AclBindingsResult> buildPlatformLevelActions(final Topology topology) {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();
    Platform platform = topology.getPlatform();

    // Set cluster level ACLs
    syncClusterLevelRbac(platform.getKafka().getRbac(), KAFKA, aclBindingsResults);
    syncClusterLevelRbac(platform.getKafkaConnect().getRbac(), KAFKA_CONNECT, aclBindingsResults);
    syncClusterLevelRbac(
        platform.getSchemaRegistry().getRbac(), SCHEMA_REGISTRY, aclBindingsResults);

    // Set component level ACLs
    for (SchemaRegistryInstance schemaRegistry : platform.getSchemaRegistry().getInstances()) {
      aclBindingsResults.add(
          new SchemaRegistryAclBindingsBuilder(bindingsBuilder, schemaRegistry).getAclBindings());
    }
    for (ControlCenterInstance controlCenter : platform.getControlCenter().getInstances()) {
      aclBindingsResults.add(
          new ControlCenterAclBindingsBuilder(bindingsBuilder, controlCenter).getAclBindings());
    }

    for (KsqlServerInstance ksqlServer : platform.getKsqlServer().getInstances()) {
      aclBindingsResults.add(
          new KSqlServerAclBindingsBuilder(bindingsBuilder, ksqlServer).getAclBindings());
    }

    return aclBindingsResults;
  }

  private void syncClusterLevelRbac(
      Optional<Map<String, List<User>>> rbac,
      Component cmp,
      List<AclBindingsResult> aclBindingsResults) {
    if (rbac.isPresent()) {
      Map<String, List<User>> roles = rbac.get();
      for (String role : roles.keySet()) {
        for (User user : roles.get(role)) {
          aclBindingsResults.add(
              new ClusterLevelAclBindingsBuilder(bindingsBuilder, role, user, cmp)
                  .getAclBindings());
        }
      }
    }
  }

  private void syncRbacRawRoles(
      Map<String, List<String>> rbacRawRoles,
      String topicPrefix,
      List<AclBindingsResult> aclBindingsResults) {
    rbacRawRoles.forEach(
        (predefinedRole, principals) ->
            principals.forEach(
                principal ->
                    aclBindingsResults.add(
                        new PredefinedAclBindingsBuilder(
                                bindingsBuilder, principal, predefinedRole, topicPrefix)
                            .getAclBindings())));
  }

  private Optional<AclBindingsResult> syncApplicationAcls(DynamicUser app, String topicPrefix) {
    AclBindingsResult aclBindingsResult = null;
    if (app instanceof KStream) {
      aclBindingsResult =
          new KStreamsAclBindingsBuilder(bindingsBuilder, (KStream) app, topicPrefix)
              .getAclBindings();
    } else if (app instanceof Connector) {
      aclBindingsResult =
          new KConnectAclBindingsBuilder(bindingsBuilder, (Connector) app, topicPrefix)
              .getAclBindings();
    } else if (app instanceof KSqlApp) {
      aclBindingsResult =
          new KSqlAppAclBindingsBuilder(bindingsBuilder, (KSqlApp) app, topicPrefix)
              .getAclBindings();
    }
    return Optional.ofNullable(aclBindingsResult);
  }

  @Override
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
