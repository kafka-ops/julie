package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AccessControlManager {

  private AccessControlProvider controlProvider;
  private ClusterState clusterState;

  public AccessControlManager(AccessControlProvider controlProvider) {
    this(controlProvider, new ClusterState());
  }

  public AccessControlManager(AccessControlProvider controlProvider, ClusterState clusterState) {
    this.controlProvider = controlProvider;
    this.clusterState = clusterState;
  }

  public void sync(final Topology topology) {

    controlProvider.clearAcls(clusterState);

    topology
        .getProjects()
        .forEach(project -> {
          project
          .getTopics()
          .forEach(topic -> {
            final String fullTopicName = topic.toString();

            Collection<String> consumerPrincipals = extractUsersToPrincipals(project.getConsumers());

            List<TopologyAclBinding> consumerBindings = controlProvider
                .setAclsForConsumers(consumerPrincipals, fullTopicName);
            clusterState.update(consumerBindings);

            Collection<String> producerPrincipals = extractUsersToPrincipals(project.getProducers());
            List<TopologyAclBinding> producerBindings = controlProvider
                .setAclsForProducers(producerPrincipals, fullTopicName);
            clusterState.update(producerBindings);
          });
          // Setup global Kafka Stream Access control lists
          String topicPrefix = project.buildTopicPrefix(topology);
          project
              .getStreams()
              .forEach(app -> {
                syncApplicationAcls(app, topicPrefix);
              });
          project
              .getConnectors()
              .forEach(connector -> {
                syncApplicationAcls(connector, topicPrefix);
              });
          project
              .getRbacRawRoles()
              .forEach((predefinedRole, principals) -> principals
                  .forEach(principal -> controlProvider
                      .setPredefinedRole(principal, predefinedRole, topicPrefix)));
        });

    clusterState.flushAndClose();
  }

  private void syncApplicationAcls(DynamicUser app, String topicPrefix) {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);
    if (app instanceof KStream) {
      controlProvider.setAclsForStreamsApp(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    } else if (app instanceof Connector) {
      controlProvider.setAclsForConnect(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    }
  }

  private Collection<String> extractUsersToPrincipals(List<? extends User> users) {
    return users
        .stream()
        .map( user -> user.getPrincipal())
        .collect(Collectors.toList());
  }

}
