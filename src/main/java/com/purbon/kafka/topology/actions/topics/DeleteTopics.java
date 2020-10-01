package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteTopics extends BaseAction {

  private static final Logger LOGGER = LogManager.getLogger(DeleteTopics.class);

  private final List<String> topicsToBeDeleted;
  private final TopologyBuilderAdminClient adminClient;

  public DeleteTopics(TopologyBuilderAdminClient adminClient, List<String> topicsToBeDeleted) {
    this.topicsToBeDeleted = topicsToBeDeleted;
    this.adminClient = adminClient;
  }

  @Override
  public void run() throws IOException {
    LOGGER.debug("Delete topics: " + topicsToBeDeleted);
    adminClient.deleteTopics(topicsToBeDeleted);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("topics", topicsToBeDeleted);
    return map;
  }
}
