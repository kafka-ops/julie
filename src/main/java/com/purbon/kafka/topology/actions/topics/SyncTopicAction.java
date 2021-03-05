package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.schema.Subject;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SyncTopicAction extends BaseAction {

  private static final Logger LOGGER = LogManager.getLogger(SyncTopicAction.class);

  private final Topic topic;
  private final String fullTopicName;
  private final TopologyBuilderAdminClient adminClient;
  private final SchemaRegistryManager schemaRegistryManager;
  private final Configuration config;
  private final boolean topicExists;

  public SyncTopicAction(
      TopologyBuilderAdminClient adminClient,
      SchemaRegistryManager schemaRegistryManager,
      Configuration config,
      Topic topic,
      String fullTopicName,
      boolean topicExists) {
    this.topic = topic;
    this.fullTopicName = fullTopicName;
    this.adminClient = adminClient;
    this.schemaRegistryManager = schemaRegistryManager;
    this.topicExists = topicExists;
    this.config = config;
  }

  public String getTopic() {
    return fullTopicName;
  }

  @Override
  public void run() throws IOException {
    syncTopic(topic, fullTopicName);
  }

  private boolean topicExists() {
    return topicExists;
  }

  private void syncTopic(Topic topic, String fullTopicName) throws IOException {
    LOGGER.debug("Sync topic {}", fullTopicName);
    if (topicExists()) {
      if (topic.partitionsCount() > adminClient.getPartitionCount(fullTopicName)) {
        LOGGER.debug("Update partition count of topic {}", fullTopicName);
        adminClient.updatePartitionCount(topic, fullTopicName);
      } else {
        LOGGER.info(
            "Skipping topic sync for topic {} due to limitation to decrease partition count of existing topic configuration",
            fullTopicName);
      }
      adminClient.updateTopicConfig(topic, fullTopicName, config.isDryRun());
    } else {
      LOGGER.debug("Create new topic with name {}", fullTopicName);
      adminClient.createTopic(topic, fullTopicName);
    }

    for (TopicSchemas schema : topic.getSchemas()) {
      Subject keySubject = schema.getKeySubject();
      Subject valueSubject = schema.getValueSubject();
      if (keySubject.hasSchemaFile()) {
        String keySchemaFile = keySubject.getSchemaFile();
        String subjectName = keySubject.buildSubjectName(topic);
        schemaRegistryManager.register(subjectName, keySchemaFile, keySubject.getFormat());
        setCompatibility(subjectName, keySubject.getOptionalCompatibility());
      }
      if (valueSubject.hasSchemaFile()) {
        String valueSchemaFile = valueSubject.getSchemaFile();
        String subjectName = valueSubject.buildSubjectName(topic);
        schemaRegistryManager.register(subjectName, valueSchemaFile, valueSubject.getFormat());
        setCompatibility(subjectName, valueSubject.getOptionalCompatibility());
      }
    }
  }

  private void setCompatibility(String subjectName, Optional<String> compatibilityOptional) {
    compatibilityOptional.ifPresent(
        compatibility -> schemaRegistryManager.setCompatibility(subjectName, compatibility));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Topic", fullTopicName);
    String actionName = topicExists() ? "update" : "create";
    Map<String, String> changes = resolveChanges();
    if (!changes.isEmpty()) {
      map.put("Action", actionName);
      map.put("Changes", changes);
    } else {
      return null;
    }
    return map;
  }

  private Map<String, String> resolveChanges() {
    try {
      return this.adminClient.updateTopicConfig(this.topic, this.fullTopicName, true);
    } catch (IOException e) {
      LOGGER.error(e);
      throw new RuntimeException(e);
    }
  }
}
