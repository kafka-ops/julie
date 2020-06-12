package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccessControlManager {

  private static final Logger LOGGER = LogManager.getLogger(AccessControlManager.class);
  private static final Boolean DEFAULT_ALLOW_DELETE = true;

  private final AccessControlProvider accessControlProvider;
  private final ClusterStateManager clusterStateManager;
  private final Boolean allowDelete;

  public AccessControlManager(AccessControlProvider accessControlProvider) {
    this(accessControlProvider, new ClusterStateManager(), DEFAULT_ALLOW_DELETE);
  }

  public AccessControlManager(AccessControlProvider accessControlProvider, Boolean allowDelete) {
    this(accessControlProvider, new ClusterStateManager(), allowDelete);
  }

  public AccessControlManager(
      AccessControlProvider accessControlProvider, ClusterStateManager clusterStateManager) {
    this(accessControlProvider, clusterStateManager, DEFAULT_ALLOW_DELETE);
  }

  public AccessControlManager(
      AccessControlProvider accessControlProvider,
      ClusterStateManager clusterStateManager,
      Boolean allowDelete) {
    this.accessControlProvider = accessControlProvider;
    this.clusterStateManager = clusterStateManager;
    this.allowDelete = allowDelete;
  }

  public void clearAcls() {
    try {
      clusterStateManager.load();
      if (allowDelete) {
        accessControlProvider.clearAcls(clusterStateManager);
      }
    } catch (Exception e) {
      LOGGER.error(e);
    } finally {
      clusterStateManager.reset();
    }
  }

  public void sync(final Topology topology) {

    clearAcls();

    topology
        .getProjects()
        .forEach(
            project -> {
              project
                  .getTopics()
                  .forEach(
                      topic -> {
                        final String fullTopicName = topic.toString();

                        Collection<String> consumerPrincipals =
                            extractUsersToPrincipals(project.getConsumers());

                        List<TopologyAclBinding> consumerBindings =
                            accessControlProvider.setAclsForConsumers(
                                consumerPrincipals, fullTopicName);
                        clusterStateManager.update(consumerBindings);

                        Collection<String> producerPrincipals =
                            extractUsersToPrincipals(project.getProducers());
                        List<TopologyAclBinding> producerBindings =
                            accessControlProvider.setAclsForProducers(
                                producerPrincipals, fullTopicName);
                        clusterStateManager.update(producerBindings);
                      });
              // Setup global Kafka Stream Access control lists
              String topicPrefix = project.buildTopicPrefix(topology);
              project
                  .getStreams()
                  .forEach(
                      app -> {
                        syncApplicationAcls(app, topicPrefix);
                      });
              project
                  .getConnectors()
                  .forEach(
                      connector -> {
                        syncApplicationAcls(connector, topicPrefix);
                      });
              syncRbacRawRoles(project.getRbacRawRoles(), topicPrefix);
            });

    syncPlatformAcls(topology);
    clusterStateManager.flushAndClose();
  }

  private void syncPlatformAcls(final Topology topology) {
    // Sync platform relevant Access Control List.
    topology
        .getPlatform()
        .getSchemaRegistry()
        .forEach(
            schemaRegistry -> {
              List<TopologyAclBinding> bindings =
                  accessControlProvider.setAclsForSchemaRegistry(schemaRegistry.getPrincipal());
              clusterStateManager.update(bindings);
            });
  }

  private void syncRbacRawRoles(Map<String, List<String>> rbacRawRoles, String topicPrefix) {
    rbacRawRoles.forEach(
        (predefinedRole, principals) ->
            principals.forEach(
                principal ->
                    accessControlProvider.setPredefinedRole(
                        principal, predefinedRole, topicPrefix)));
  }

  private void syncApplicationAcls(DynamicUser app, String topicPrefix) {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);
    List<TopologyAclBinding> bindings = new ArrayList<>();
    if (app instanceof KStream) {
      bindings =
          accessControlProvider.setAclsForStreamsApp(
              app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    } else if (app instanceof Connector) {
      bindings =
          accessControlProvider.setAclsForConnect(
              app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    }
    clusterStateManager.update(bindings);
  }

  private Collection<String> extractUsersToPrincipals(List<? extends User> users) {
    return users.stream().map(User::getPrincipal).collect(Collectors.toList());
  }

  public void printCurrentState(PrintStream out) {
    out.println("List of ACLs: ");
    accessControlProvider
        .listAcls()
        .forEach(
            (topic, aclBindings) -> {
              out.println(topic);
              aclBindings.forEach(out::println);
            });
  }
}
