package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.KStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AclsManager {

  private final TopologyBuilderAdminClient adminClient;

  public AclsManager(final TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void syncAcls(final Topology topology) {

    topology
        .getProjects()
        .stream()
        .forEach(new Consumer<Project>() {
          @Override
          public void accept(Project project) {
            project
            .getTopics()
            .stream()
            .forEach(new Consumer<Topic>() {
              @Override
              public void accept(Topic topic) {
                final String fullTopicName = topic.composeTopicName(topology, project.getName());
                final Collection<String> consumers = project
                    .getConsumers()
                    .stream()
                    .map(consumer -> consumer.getPrincipal())
                    .collect(Collectors.toList());
                setAclsForConsumers(consumers, fullTopicName);

                final Collection<String> producers = project
                    .getProducers()
                    .stream()
                    .map(producer -> producer.getPrincipal())
                    .collect(Collectors.toList());
                setAclsForProducers(producers, fullTopicName);
              }
            });
            // Setup global Kafka Stream Access control lists
            String topicPrefix = project.buildTopicPrefix(topology);
            project
                .getStreams()
                .stream()
                .forEach(app -> {
                  List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
                  List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);
                  setAclsForStreamsApp(app.getPrincipal(), topicPrefix, readTopics, writeTopics);
                });
          }
        });
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
