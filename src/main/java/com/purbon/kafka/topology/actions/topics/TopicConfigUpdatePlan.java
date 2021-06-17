package com.purbon.kafka.topology.actions.topics;

import java.util.HashMap;
import java.util.Map;

public class TopicConfigUpdatePlan {
  private String topicName;
  private boolean updatePartitionCount;
  private Map<String, String> newConfigValues = new HashMap<>();
  private Map<String, String> updatedConfigValues = new HashMap<>();
  private Map<String, String> deletedConfigValues = new HashMap<>();

  public TopicConfigUpdatePlan(String topicName) {
    this.topicName = topicName;
  }

  public void addNewConfig(String name, String value) {
    newConfigValues.put(name, value);
  }

  public void addConfigToUpdate(String name, String value) {
    newConfigValues.put(name, value);
  }

  public void addConfigToDelete(String name, String value) {
    newConfigValues.put(name, value);
  }

  public boolean isUpdatePartitionCount() {
    return updatePartitionCount;
  }

  public void setUpdatePartitionCount(boolean updatePartitionCount) {
    this.updatePartitionCount = updatePartitionCount;
  }
}
