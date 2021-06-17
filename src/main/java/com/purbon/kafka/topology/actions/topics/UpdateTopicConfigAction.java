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

  private final Topic topic;
  private final String fullTopicName;
  private final TopologyBuilderAdminClient adminClient;

  public UpdateTopicConfigAction(
      TopologyBuilderAdminClient adminClient, Topic topic, String fullTopicName) {
    this.topic = topic;
    this.fullTopicName = fullTopicName;
    this.adminClient = adminClient;
  }

  public String getTopic() {
    return fullTopicName;
  }

  @Override
  public void run() throws IOException {
    updateConfig(topic, fullTopicName);
  }

  private void updateConfig(Topic topic, String fullTopicName) throws IOException {
    LOGGER.debug(String.format("Update config for topic %s", fullTopicName));
    if (topic.partitionsCount() > adminClient.getPartitionCount(fullTopicName)) {
      LOGGER.debug(String.format("Update partition count of topic %s", fullTopicName));
      adminClient.updatePartitionCount(topic, fullTopicName);
    }
    adminClient.updateTopicConfig(topic, fullTopicName);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Topic", fullTopicName);
    map.put("Action", "update");
    return map;
  }
}
