package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopicManager {

  public static final String NUM_PARTITIONS = "num.partitions";
  public static final String REPLICATION_FACTOR = "replication.factor";

  private final TopologyBuilderAdminClient adminClient;

  public TopicManager(TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void sync(Topology topology) {

    //List all topics existing in the cluster
    Set<String> listOfTopics = adminClient.listTopics();

    Set<String> updatedListOfTopics = new HashSet<>();
    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created
    topology
        .getProjects()
        .stream()
        .forEach(project -> project
            .getTopics()
            .forEach(topic -> {
              String fullTopicName = topic.toString();
              syncTopic(topic, fullTopicName, listOfTopics);
              updatedListOfTopics.add(fullTopicName);
            }));

    // Handle topic delete: Topics in the initial list, but not present anymore after a
    // full topic sync should be deleted
    List<String> topicsToBeDeleted = new ArrayList<>();
    listOfTopics
        .stream()
        .forEach(originalTopic -> {
          if (!updatedListOfTopics.contains(originalTopic)) {
            topicsToBeDeleted.add(originalTopic);
          }
        });
    adminClient.deleteTopics(topicsToBeDeleted);
  }

  public void syncTopic(Topic topic, String fullTopicName, Set<String> listOfTopics) {
    if (existTopic(fullTopicName, listOfTopics)) {
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics, Topology topology, Project project) {
    String fullTopicName = topic.toString();
    syncTopic(topic, fullTopicName, listOfTopics);
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

}
