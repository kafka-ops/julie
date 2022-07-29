package com.purbon.kafka.topology.actions.topics.builders;

import com.purbon.kafka.topology.actions.topics.TopicConfigUpdatePlan;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import java.io.IOException;
import java.util.HashMap;
import org.apache.kafka.clients.admin.Config;

public class TopicConfigUpdatePlanBuilder {

  private TopologyBuilderAdminClient adminClient;

  public TopicConfigUpdatePlanBuilder(TopologyBuilderAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public TopicConfigUpdatePlan createTopicConfigUpdatePlan(Topic topic, String fullTopicName) {

    Config currentKafkaConfigs = adminClient.getActualTopicConfig(fullTopicName);

    TopicConfigUpdatePlan topicConfigUpdatePlan = new TopicConfigUpdatePlan(topic);

    try {
      if (topic.partitionsCount() > adminClient.getPartitionCount(fullTopicName)) {
        topicConfigUpdatePlan.setUpdatePartitionCount(true);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to get partition count for topic " + fullTopicName, e);
    }

    HashMap<String, String> topicConfigs = topic.getRawConfig();

    topicConfigUpdatePlan.addNewOrUpdatedConfigs(topicConfigs, currentKafkaConfigs);
    topicConfigUpdatePlan.addDeletedConfigs(topicConfigs, currentKafkaConfigs);

    return topicConfigUpdatePlan;
  }
}
