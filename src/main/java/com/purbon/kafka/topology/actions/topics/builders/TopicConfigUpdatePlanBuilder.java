package com.purbon.kafka.topology.actions.topics.builders;

import com.purbon.kafka.topology.actions.topics.TopicConfigUpdatePlan;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import java.io.IOException;
import java.util.Set;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;

public class TopicConfigUpdatePlanBuilder {

  private TopologyBuilderAdminClient adminClient;

  public TopicConfigUpdatePlanBuilder(TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public TopicConfigUpdatePlan createTopicConfigUpdatePlan(Topic topic, String fullTopicName) {

    Config currentConfigs = adminClient.getActualTopicConfig(fullTopicName);

    TopicConfigUpdatePlan topicConfigUpdatePlan = new TopicConfigUpdatePlan(topic);

    try {
      if (topic.partitionsCount() > adminClient.getPartitionCount(fullTopicName)) {
        topicConfigUpdatePlan.setUpdatePartitionCount(true);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to get partition count for topic " + fullTopicName, e);
    }

    topic
        .getRawConfig()
        .forEach(
            (configKey, configValue) -> {
              ConfigEntry currentConfigEntry = currentConfigs.get(configKey);
              //TODO: Must analyze further if isDynamicTopicConfig is the correct thing here
              if (!isDynamicTopicConfig(currentConfigEntry) && !currentConfigEntry.value().equals(configValue)) {
                topicConfigUpdatePlan.addNewConfig(configKey, configValue);
              } else if (!currentConfigEntry.value().equals(configValue)) {
                  topicConfigUpdatePlan.addConfigToUpdate(configKey, configValue);
              }

              Set<String> configKeys = topic.getRawConfig().keySet();

              currentConfigs
                  .entries()
                  .forEach(
                      entry -> {
                          //TODO: This must check on other config sources as well - must be analyzed
                        if (!entry.isDefault() && !configKeys.contains(entry.name())) {
                          topicConfigUpdatePlan.addConfigToDelete(entry.name(), entry.value());
                        }
                      });
            });

    return topicConfigUpdatePlan;
  }

    private boolean isDynamicTopicConfig(ConfigEntry currentConfigEntry) {
        return currentConfigEntry.source().equals(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG);
    }
}
