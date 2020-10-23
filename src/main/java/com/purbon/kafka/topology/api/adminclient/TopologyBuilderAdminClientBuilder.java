package com.purbon.kafka.topology.api.adminclient;

import com.purbon.kafka.topology.TopologyBuilderConfig;
import java.io.IOException;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyBuilderAdminClientBuilder {

  private static final Logger LOGGER =
      LogManager.getLogger(TopologyBuilderAdminClientBuilder.class);

  private final TopologyBuilderConfig config;

  public TopologyBuilderAdminClientBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  public TopologyBuilderAdminClient build() throws IOException {
    Properties props = config.asProperties();
    LOGGER.info(
        String.format(
            "Connecting AdminClient to %s",
            props.getProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG)));
    TopologyBuilderAdminClient client = new TopologyBuilderAdminClient(AdminClient.create(props));
    if (!config.isDryRun()) {
      client.healthCheck();
    }
    return client;
  }
}
