package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteTopics implements Action {

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
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("topics", topicsToBeDeleted);
    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
