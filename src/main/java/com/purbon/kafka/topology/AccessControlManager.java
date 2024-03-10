package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.model.Component.*;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.access.CreateBindings;
import com.purbon.kafka.topology.actions.access.builders.*;
import com.purbon.kafka.topology.actions.access.builders.rbac.*;
import com.purbon.kafka.topology.exceptions.RemoteValidationException;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.users.*;
import com.purbon.kafka.topology.model.users.platform.*;
import com.purbon.kafka.topology.roles.ResourceFilter;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);

  private final Configuration config;
  private final JulieRoles julieRoles;
  private AccessControlProvider controlProvider;
  private BindingsBuilderProvider bindingsBuilder;
  private final ResourceFilter resourceFilter;

  public AccessControlManager(
      AccessControlProvider controlProvider, BindingsBuilderProvider builderProvider) {
    this(controlProvider, builderProvider, new Configuration());
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      BindingsBuilderProvider builderProvider,
      Configuration config) {
    this(controlProvider, builderProvider, new JulieRoles(), config);
  }

  public AccessControlManager(
      AccessControlProvider controlProvider,
      BindingsBuilderProvider builderProvider,
      JulieRoles julieRoles,
      Configuration config) {
    this.controlProvider = controlProvider;
    this.bindingsBuilder = builderProvider;
    this.config = config;
    this.julieRoles = julieRoles;
    this.resourceFilter = new ResourceFilter(config);
  }

  @Override
  public void updatePlan(ExecutionPlan plan, final Map<String, Topology> topologies)
      throws IOException {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();
    for (Topology topology : topologies.values()) {
      julieRoles.validateTopology(topology);
      aclBindingsResults.addAll(buildProjectAclBindings(topology));
      aclBindingsResults.addAll(buildPlatformLevelActions(topology));
      aclBindingsResults.addAll(buildSpecialTopicsAcls(topology));
    }

    buildUpdateBindingsActions(aclBindingsResults, loadActualClusterStateIfAvailable(plan))
        .forEach(plan::add);
  }

  private Set<TopologyAclBinding> loadActualClusterStateIfAvailable(ExecutionPlan plan)
      throws IOException {
    Set<TopologyAclBinding> bindings =
        config.fetchStateFromTheCluster() ? providerBindings() : plan.getBindings();
    var currentState =
        bindings.stream()
            .filter(resourceFilter::matchesManagedPrefixList)
            .filter(this::isNotInternalAcl)
            .collect(Collectors.toSet());

    if (!config.shouldVerifyRemoteState()) {
      OnceOnlyWarningLogger.getInstance().logRemoteStateVerificationDisabledWarning();
    }

    if (config.shouldVerifyRemoteState() && !config.fetchStateFromTheCluster()) {
      // should detect if there are divergences between the local cluster state and the current
      // status in the cluster
      detectDivergencesInTheRemoteCluster(plan);
    }

    return currentState;
  }

  private void detectDivergencesInTheRemoteCluster(ExecutionPlan plan)
      throws RemoteValidationException {
    if (!config.isAllowDeleteTopics()) {
      /* Assume topics are cleaned up by mechanisms outside JulieOps, and do not fail. */
      return;
    }
    var remoteAcls = providerBindings();

    var delta =
        plan.getBindings().stream()
            .filter(acl -> !remoteAcls.contains(acl))
            .collect(Collectors.toList());

    if (delta.size() > 0) {
      String errorMessage =
          "Your remote state has changed since the last execution, this ACL(s): "
              + StringUtils.join(delta, ",")
              + " are in your local state, but not in the cluster, please investigate!";
      LOGGER.error(errorMessage);
      throw new RemoteValidationException(errorMessage);
    }
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
                (list) ->
                    aclBindingsResults.add(
                        new ConnectorAuthorizationAclBindingsBuilder(bindingsBuilder, connector)
                            .getAclBindings()));
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        aclBindingsResults.add(
            new SchemaAuthorizationAclBindingsBuilder(
                    new BuildBindingsForSchemaAuthorization(
                        bindingsBuilder, schemaAuthorization, config, topicPrefix))
                .getAclBindings());
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix, aclBindingsResults);

      for (Map.Entry<String, List<Other>> other : project.getOthers().entrySet()) {
        if (julieRoles.size() == 0) {
          throw new IllegalStateException(
              "Custom JulieRoles are being used without providing the required config file.");
        }
        BuildBindingsForRole buildBindingsForRole =
            new BuildBindingsForRole(
                bindingsBuilder, julieRoles.get(other.getKey()), other.getValue());
        try {
          buildBindingsForRole.run();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        aclBindingsResults.add(
            AclBindingsResult.forAclBindings(buildBindingsForRole.getAclBindings()));
      }
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
    return buildBasicUsersAcls(project.getTopics(), project, includeProjectLevel);
  }

  private List<AclBindingsResult> buildSpecialTopicsAcls(Topology topology) {
    return buildBasicUsersAcls(topology.getSpecialTopics(), null, false);
  }

  private List<AclBindingsResult> buildBasicUsersAcls(
      Collection<Topic> topics, Project project, boolean includeProjectLevel) {
    List<AclBindingsResult> aclBindingsResults = new ArrayList<>();

    for (Topic topic : topics) {
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
    }
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
            .filter(resourceFilter::matchesManagedPrefixList)
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
