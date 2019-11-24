package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.KStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AclsManager {

  private final TopologyBuilderAdminClient adminClient;

  public AclsManager(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void sync(final Topology topology) {
    topology
        .getProjects()
        .forEach(project -> {
          project
          .getTopics()
          .forEach(topic -> {
            final String fullTopicName = topic.toString();

            Collection<String> consumerPrincipals = extractUsersToPrincipals(project.getConsumers());
            setAclsForConsumers(consumerPrincipals, fullTopicName);

            Collection<String> producerPrincipals = extractUsersToPrincipals(project.getProducers());
            setAclsForProducers(producerPrincipals, fullTopicName);

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
      setAclsForStreamsApp(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    } else if (app instanceof Connector) {
      setAclsForConnect(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
    }
  }

  private Collection<String> extractUsersToPrincipals(List<? extends User> users) {
    return users
        .stream()
        .map( user -> user.getPrincipal())
        .collect(Collectors.toList());
  }

  private void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {
    adminClient
        .setAclsForConnect(principal, topicPrefix, readTopics, writeTopics);
  }

  private void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    adminClient
        .setAclsForStreamsApp(principal, topicPrefix, readTopics, writeTopics);
  }

  public void setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForConsumer(principal, topic));
  }

  public void setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForProducer(principal, topic));
  }


}
