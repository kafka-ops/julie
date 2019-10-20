package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;

public class TopicManager {

  private final TopologyBuilderAdminClient adminClient;

  public TopicManager(TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void syncTopics(Topology topology) {

    //List all topics existing in the cluster
    Set<String> listOfTopics = adminClient.listTopics();

    // Foreach topic in the topology, sync it's content
    // if topics does not exist already it's created
    topology
        .getProjects()
        .stream()
        .forEach(project -> project
            .getTopics()
            .forEach(topic -> {
              syncTopic(topic, listOfTopics, topology, project);
            }));
    // TODO: Add code to delete topics not anymore present in the list
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics, Topology topology, Project project) {
    String fullTopicName = topic.composeTopicName(topology, project.getName());
    if (existTopic(fullTopicName, listOfTopics)) {
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

}
