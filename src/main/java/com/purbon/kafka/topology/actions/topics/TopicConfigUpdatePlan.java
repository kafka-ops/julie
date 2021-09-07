package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.model.Topic;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;

public class TopicConfigUpdatePlan {
  private final Topic topic;
  private boolean updatePartitionCount;
  private Map<String, String> newConfigValues = new HashMap<>();
  private Map<String, String> updatedConfigValues = new HashMap<>();
  private Map<String, String> deletedConfigValues = new HashMap<>();

  public TopicConfigUpdatePlan(Topic topic) {
    this.topic = topic;
  }

  public void addNewConfig(String name, String value) {
    newConfigValues.put(name, value);
  }

  public void addConfigToUpdate(String name, String value) {
    updatedConfigValues.put(name, value);
  }

  public void addConfigToDelete(String name, String value) {
    deletedConfigValues.put(name, value);
  }

  public Topic getTopic() {
    return topic;
  }

  public String getFullTopicName() {
    return topic.toString();
  }

  public Map<String, String> getNewConfigValues() {
    return newConfigValues;
  }

  public Map<String, String> getUpdatedConfigValues() {
    return updatedConfigValues;
  }

  public Map<String, String> getDeletedConfigValues() {
    return deletedConfigValues;
  }

  public boolean hasNewConfigs() {
    return !newConfigValues.isEmpty();
  }

  public boolean hasUpdatedConfigs() {
    return !updatedConfigValues.isEmpty();
  }

  public boolean hasDeletedConfigs() {
    return !deletedConfigValues.isEmpty();
  }

  public Integer getTopicPartitionCount() {
    return topic.partitionsCount();
  }

  public boolean isUpdatePartitionCount() {
    return updatePartitionCount;
  }

  public void setUpdatePartitionCount(boolean updatePartitionCount) {
    this.updatePartitionCount = updatePartitionCount;
  }

  public boolean hasConfigChanges() {
    return updatePartitionCount || hasNewConfigs() || hasUpdatedConfigs() || hasDeletedConfigs();
  }

  public void addNewOrUpdatedConfigs(
      HashMap<String, String> topicConfigs, Config currentKafkaConfigs) {
    topicConfigs.forEach(
        (configKey, configValue) -> {
          ConfigEntry currentConfigEntry = currentKafkaConfigs.get(configKey);
          if (!currentConfigEntry.value().equals(configValue)) {
            if (isDynamicTopicConfig(currentConfigEntry)) {
              addConfigToUpdate(configKey, configValue);
            } else {
              addNewConfig(configKey, configValue);
            }
          }
        });
  }

  public void addDeletedConfigs(HashMap<String, String> topicConfigs, Config currentKafkaConfigs) {
    Set<String> configKeys = topicConfigs.keySet();
    currentKafkaConfigs
        .entries()
        .forEach(
            entry -> {
              if (isDynamicTopicConfig(entry) && !configKeys.contains(entry.name())) {
                addConfigToDelete(entry.name(), entry.value());
              }
            });
  }

  private boolean isDynamicTopicConfig(ConfigEntry currentConfigEntry) {
    return currentConfigEntry.source().equals(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG);
  }
}
