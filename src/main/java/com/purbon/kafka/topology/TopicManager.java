package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Collection;
import java.util.Collections;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;

public class TopicManager {

  private final KafkaAdminClient adminClient;

  public TopicManager(KafkaAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void syncTopics(Topology topology) {

    topology
        .getProjects()
        .stream()
        .forEach(project -> project
            .getTopics()
            .forEach(topic -> syncTopic(topic)));
  }

  public void syncTopic(Topic topic) {

    int numPartitions = Integer.valueOf(topic.getConfig().getOrDefault("num.partitions", "3");
    short replicationFactor = Short.valueOf(topic.getConfig().getOrDefault("replicationFactor", "2"));

    NewTopic newTopic = new NewTopic(topic.getName(), numPartitions, replicationFactor)
        .configs(topic.getConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    adminClient.createTopics(newTopics);

  }
}
