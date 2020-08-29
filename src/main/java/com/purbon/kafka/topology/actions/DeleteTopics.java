package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import java.io.IOException;
import java.util.List;

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
}
