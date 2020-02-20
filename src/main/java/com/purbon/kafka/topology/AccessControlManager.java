package com.purbon.kafka.topology;

import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.KStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AccessControlManager {

  private final AccessControlProvider accessControlProvider;

  public AccessControlManager(final AccessControlProvider accessControlProvider) {
    this.accessControlProvider = accessControlProvider;
  }

  public void sync(final Topology topology) {

    accessControlProvider.clearAcls();

    topology
        .getProjects()
        .forEach(project -> {
          project
          .getTopics()
          .forEach(topic -> {
            final String fullTopicName = topic.toString();

            Collection<String> consumerPrincipals = extractUsersToPrincipals(project.getConsumers());
            accessControlProvider.setAclsForConsumers(consumerPrincipals, fullTopicName);

            Collection<String> producerPrincipals = extractUsersToPrincipals(project.getProducers());
            accessControlProvider.setAclsForProducers(producerPrincipals, fullTopicName);
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
        });
  }

  private void syncApplicationAcls(DynamicUser app, String topicPrefix) {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);
    if (app instanceof KStream) {
      accessControlProvider.setAclsForStreamsApp(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    } else if (app instanceof Connector) {
      accessControlProvider.setAclsForConnect(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    }
  }

  private Collection<String> extractUsersToPrincipals(List<? extends User> users) {
    return users
        .stream()
        .map( user -> user.getPrincipal())
        .collect(Collectors.toList());
  }

}
