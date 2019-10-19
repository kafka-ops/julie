package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;

public class TopicManager {

  private final KafkaAdminClient adminClient;

  public TopicManager(KafkaAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void syncTopics(Topology topology) {

    //List all topics existing in the cluster
    Set<String> listOfTopics = listTopics();

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

  public Set<String> listTopics() {
    Set<String> listOfTopics = new HashSet<>();
    try {
      listOfTopics = adminClient
          .listTopics()
          .names()
          .get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    return listOfTopics;

  }
  private String composeTopicName(Topology topology, String projectName, Topic topic) {
    StringBuilder sb = new StringBuilder();
    sb.append(topology.getTeam());
    sb.append(".");
    sb.append(topology.getSource());
    sb.append(".");
    sb.append(projectName);
    sb.append(".");
    sb.append(topic.getName());
    return sb.toString();
  }


  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics, Topology topology, Project project) {
    String fullTopicName = composeTopicName(topology, project.getName(), topic);
    if (existTopic(fullTopicName, listOfTopics)) {
      updateTopicConfig(topic, fullTopicName);
    } else {
      createTopic(topic, fullTopicName);
    }
  }

  private void updateTopicConfig(Topic topic, String fullTopicName) {

    Map<ConfigResource,Collection<AlterConfigOp>> configs = new HashMap<>();

    topic
        .getConfig()
        .forEach(new BiConsumer<String, String>() {
          @Override
          public void accept(String configKey, String configValue) {
            configs.put(new ConfigResource(Type.TOPIC, fullTopicName),
                Collections.singleton(new AlterConfigOp(new ConfigEntry(configKey, configValue), OpType.SET)));
          }
        });

    adminClient
        .incrementalAlterConfigs(configs);
  }

  private void createTopic(Topic topic, String fullTopicName) {
    int numPartitions = Integer.valueOf(topic.getConfig().getOrDefault("num.partitions", "3"));
    short replicationFactor = Short.valueOf(topic.getConfig().getOrDefault("replicationFactor", "2"));

    NewTopic newTopic = new NewTopic(fullTopicName, numPartitions, replicationFactor)
        .configs(topic.getConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    adminClient.createTopics(newTopics);
  }
}
