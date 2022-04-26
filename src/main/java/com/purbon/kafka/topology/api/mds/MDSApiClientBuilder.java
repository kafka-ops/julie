package com.purbon.kafka.topology.api.mds;

import com.purbon.kafka.topology.Configuration;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDSApiClientBuilder {

  private static final Logger LOGGER = LogManager.getLogger(MDSApiClientBuilder.class);

  private Configuration config;

  public MDSApiClientBuilder(Configuration config) {
    this.config = config;
  }

  public MDSApiClient build() throws IOException {
    String mdsServer = config.getMdsServer();

    MDSApiClient apiClient = new MDSApiClient(mdsServer, Optional.of(config));
    // Pass Cluster IDS
    apiClient.setKafkaClusterId(config.getKafkaClusterId());
    apiClient.setSchemaRegistryClusterID(config.getSchemaRegistryClusterId());
    apiClient.setConnectClusterID(config.getKafkaConnectClusterId());
    apiClient.setKSqlClusterID(config.getKsqlDBClusterID());

    LOGGER.debug(String.format("Connecting to an MDS server at %s", mdsServer));
    return apiClient;
  }

  public void configure(Configuration config) {
    this.config = config;
  }
}
