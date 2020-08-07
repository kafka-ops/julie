package com.purbon.kafka.topology;

import org.apache.kafka.clients.admin.AdminClient;

public class TopologyBuilderAdminClientBuilder {

  private final TopologyBuilderConfig config;

  public TopologyBuilderAdminClientBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  public TopologyBuilderAdminClient build() {
    AdminClient adminClient = AdminClient.create(config.getProperties());
    return new TopologyBuilderAdminClient(adminClient, config);
  }
}
