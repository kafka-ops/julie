package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);
  private final Boolean allowDelete;

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

    this.allowDelete = Boolean.valueOf(cliParams.getOrDefault(ALLOW_DELETE_OPTION, "true"));
  }

  public void clearAcls() {
    try {
      clusterState.load();
      if (allowDelete) {
        controlProvider.clearAcls(clusterState);
      }
    } catch (Exception e) {
      LOGGER.error(e);
    } finally {
      if (allowDelete) {
        clusterState.reset();
      }
    }
  }

  public void sync(final Topology topology) throws IOException {

    clearAcls();

    for (Project project : topology.getProjects()) {
      project
          .getTopics()
          .forEach(
              topic -> {
                final String fullTopicName = topic.toString();

                List<TopologyAclBinding> consumerBindings =
                    controlProvider.setAclsForConsumers(project.getConsumers(), fullTopicName);
                clusterState.add(consumerBindings);

                Collection<String> producerPrincipals =
                    extractUsersToPrincipals(project.getProducers());
                List<TopologyAclBinding> producerBindings =
                    controlProvider.setAclsForProducers(producerPrincipals, fullTopicName);
                clusterState.add(producerBindings);
              });
      // Setup global Kafka Stream Access control lists
      String topicPrefix = project.buildTopicPrefix(topology.buildNamePrefix());
      for (KStream app : project.getStreams()) {
        syncApplicationAcls(app, topicPrefix);
      }
      for (Connector connector : project.getConnectors()) {
        syncApplicationAcls(connector, topicPrefix);
      }

      for (Schemas schemaAuthorization : project.getSchemas()) {
        controlProvider
            .setSchemaAuthorization(
                schemaAuthorization.getPrincipal(), schemaAuthorization.getSubjects())
            .forEach(binding -> clusterState.add(binding));
      }

      syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix);
    }

    syncPlatformAcls(topology);
    clusterState.flushAndClose();
  }

  private void syncPlatformAcls(final Topology topology) throws ConfigurationException {
    // Sync platform relevant Access Control List.
    Platform platform = topology.getPlatform();
    for (SchemaRegistry schemaRegistry : platform.getSchemaRegistry()) {
      List<TopologyAclBinding> bindings = controlProvider.setAclsForSchemaRegistry(schemaRegistry);
      clusterState.add(bindings);
    }

    platform
        .getControlCenter()
        .forEach(
            controlCenter -> {
              List<TopologyAclBinding> bindings =
                  controlProvider.setAclsForControlCenter(
                      controlCenter.getPrincipal(), controlCenter.getAppId());
              clusterState.add(bindings);
            });
  }

  private void syncRbacRawRoles(Map<String, List<String>> rbacRawRoles, String topicPrefix) {
    rbacRawRoles.forEach(
        (predefinedRole, principals) ->
            principals.forEach(
                principal ->
                    controlProvider.setPredefinedRole(principal, predefinedRole, topicPrefix)));
  }

  private void syncApplicationAcls(DynamicUser app, String topicPrefix) throws IOException {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);
    List<TopologyAclBinding> bindings = new ArrayList<>();
    if (app instanceof KStream) {
      bindings =
          controlProvider.setAclsForStreamsApp(
              app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    } else if (app instanceof Connector) {
      bindings = controlProvider.setAclsForConnect((Connector) app, topicPrefix);
    }
    clusterState.add(bindings);
  }

  private Collection<String> extractUsersToPrincipals(List<? extends User> users) {
    return users.stream().map(user -> user.getPrincipal()).collect(Collectors.toList());
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
}
