package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ACLManager {

  private final TopologyBuilderAdminClient adminClient;

  public ACLManager(TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void syncAcls(Topology topology) {

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
                String fullTopicName = topic.composeTopicName(topology, project.getName());
                Collection<String> consumers = project
                    .getConsumers()
                    .stream()
                    .map(consumer -> consumer.getPrincipal())
                    .collect(Collectors.toList());
                setAclsForConsumers(consumers, fullTopicName);

                Collection<String> producers = project
                    .getProducers()
                    .stream()
                    .map(producer -> producer.getPrincipal())
                    .collect(Collectors.toList());
                setAclsForProducers(producers, fullTopicName);

              }
            });
          }
        });
  }

  public void setAclsForConsumers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForConsumer(principal, topic));
  }

  public void setAclsForProducers(Collection<String> principals, String topic) {
    principals.forEach(principal -> adminClient.setAclsForProducer(principal, topic));
  }

}
