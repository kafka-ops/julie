package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topic;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;

public class TopologyBuilderAdminClient {


  private final AdminClient adminClient;

  public TopologyBuilderAdminClient(AdminClient adminClient) {
    this.adminClient = adminClient;
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

  public void updateTopicConfig(Topic topic, String fullTopicName) {

    Map<ConfigResource,Collection<AlterConfigOp>> configs = new HashMap<>();

    topic
        .getConfig()
        .forEach(new BiConsumer<String, String>() {
          @Override
          public void accept(String configKey, String configValue) {
            configs.put(new ConfigResource(Type.TOPIC, fullTopicName),
                Collections
                    .singleton(new AlterConfigOp(new ConfigEntry(configKey, configValue), OpType.SET)));
          }
        });

    adminClient
        .incrementalAlterConfigs(configs);
  }

  public void createTopic(Topic topic, String fullTopicName) {
    int numPartitions = Integer.valueOf(topic.getConfig().getOrDefault("num.partitions", "3"));
    short replicationFactor = Short.valueOf(topic.getConfig().getOrDefault("replicationFactor", "2"));

    NewTopic newTopic = new NewTopic(fullTopicName, numPartitions, replicationFactor)
        .configs(topic.getConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    adminClient.createTopics(newTopics);
  }

  public void deleteTopic(String topic) {
    deleteTopics(Collections.singletonList(topic));
  }

  public void deleteTopics(Collection<String> topics) {
    adminClient.deleteTopics(topics);
  }
}
