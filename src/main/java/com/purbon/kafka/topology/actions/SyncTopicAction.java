package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class SyncTopicAction implements Action {

  private final Topic topic;
  private final String fullTopicName;
  private final Set<String> listOfTopics;
  private final TopologyBuilderAdminClient adminClient;
  private final SchemaRegistryManager schemaRegistryManager;

  public SyncTopicAction(
      TopologyBuilderAdminClient adminClient,
      SchemaRegistryManager schemaRegistryManager,
      Topic topic,
      String fullTopicName,
      Set<String> listOfTopics) {
    this.topic = topic;
    this.fullTopicName = fullTopicName;
    this.listOfTopics = listOfTopics;
    this.adminClient = adminClient;
    this.schemaRegistryManager = schemaRegistryManager;
  }

  @Override
  public void run() throws IOException {
    syncTopic(topic, fullTopicName, listOfTopics);
  }

  public void syncTopic(Topic topic, Set<String> listOfTopics) throws IOException {
    String fullTopicName = topic.toString();
    syncTopic(topic, fullTopicName, listOfTopics);
  }

  public void syncTopic(Topic topic, String fullTopicName, Set<String> listOfTopics)
      throws IOException {
    if (existTopic(fullTopicName, listOfTopics)) {
      if (topic.partitionsCount() > adminClient.getPartitionCount(fullTopicName)) {
        adminClient.updatePartitionCount(topic, fullTopicName);
      }
      adminClient.updateTopicConfig(topic, fullTopicName);
    } else {
      adminClient.createTopic(topic, fullTopicName);
    }

    if (topic.getSchemas() != null) {
      final TopicSchemas schemas = topic.getSchemas();

      if (StringUtils.isNotBlank(schemas.getKeySchemaFile())) {
        schemaRegistryManager.register(fullTopicName, schemas.getKeySchemaFile());
      }

      if (StringUtils.isNotBlank(schemas.getValueSchemaFile())) {
        schemaRegistryManager.register(fullTopicName, schemas.getValueSchemaFile());
      }
    }
  }

  private boolean existTopic(String topic, Set<String> listOfTopics) {
    return listOfTopics.contains(topic);
  }
}
