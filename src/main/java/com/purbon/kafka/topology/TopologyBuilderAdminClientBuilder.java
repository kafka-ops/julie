package com.purbon.kafka.topology;

import com.nerdynick.commons.configuration.utils.TransformConfigUtils;
import org.apache.kafka.clients.admin.AdminClient;

public class TopologyBuilderAdminClientBuilder {

  private final TopologyBuilderConfig config;

  public TopologyBuilderAdminClientBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  public TopologyBuilderAdminClient build() {
    AdminClient adminClient = AdminClient.create(TransformConfigUtils.toMap(config));
    return new TopologyBuilderAdminClient(adminClient, config);
  }
}
