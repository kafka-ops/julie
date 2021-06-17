package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topic;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateTopicConfigAction extends BaseAction {

  private static final Logger LOGGER = LogManager.getLogger(UpdateTopicConfigAction.class);

  private final TopicConfigUpdatePlan topicConfigUpdatePlan;
  private final TopologyBuilderAdminClient adminClient;

  public UpdateTopicConfigAction(
      TopologyBuilderAdminClient adminClient, TopicConfigUpdatePlan topicConfigUpdatePlan) {
    this.topicConfigUpdatePlan = topicConfigUpdatePlan;
    this.adminClient = adminClient;
  }

  @Override
  public void run() throws IOException {
    final Topic topic = topicConfigUpdatePlan.getTopic();
    final String fullTopicName = topicConfigUpdatePlan.getFullTopicName();

    LOGGER.debug(String.format("Update config for topic %s", fullTopicName));
    if (topicConfigUpdatePlan.isUpdatePartitionCount()) {
      LOGGER.debug(String.format("Update partition count of topic %s", fullTopicName));
      adminClient.updatePartitionCount(topic, fullTopicName);
    }

    adminClient.updateTopicConfig(topicConfigUpdatePlan);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Topic", topicConfigUpdatePlan.getFullTopicName());
    map.put("Action", "update");
    return map;
  }
}
