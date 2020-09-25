package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteTopics extends BaseAction {

  private final List<String> topicsToBeDeleted;
  private final TopologyBuilderAdminClient adminClient;

  public DeleteTopics(TopologyBuilderAdminClient adminClient, List<String> topicsToBeDeleted) {
    this.topicsToBeDeleted = topicsToBeDeleted;
    this.adminClient = adminClient;
  }

  @Override
  public void run() throws IOException {
    adminClient.deleteTopics(topicsToBeDeleted);
  }

  @Override
  Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("topics", topicsToBeDeleted);
    return map;
  }
}
