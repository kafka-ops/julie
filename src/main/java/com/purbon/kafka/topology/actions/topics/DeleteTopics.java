package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("Operation", getClass().getName());
    map.put("topics", topicsToBeDeleted);
    return map;
  }

  public List<String> getTopicsToBeDeleted() {
    return topicsToBeDeleted;
  }

  @Override
  protected List<Map<String, Object>> detailedProps() {
    return topicsToBeDeleted.stream()
        .map(
            new Function<String, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(String topic) {
                Map<String, Object> map = new HashMap<>();
                map.put(
                    "resource_name",
                    String.format("rn://delete.topic/%s/%s", getClass().getName(), topic));
                map.put("operation", getClass().getName());
                map.put("topic", topic);
                return map;
              }
            })
        .collect(Collectors.toList());
  }
}
