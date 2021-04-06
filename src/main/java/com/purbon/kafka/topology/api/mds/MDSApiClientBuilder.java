package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.Constants.*;

import com.purbon.kafka.topology.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDSApiClientBuilder {

  private static final Logger LOGGER = LogManager.getLogger(MDSApiClientBuilder.class);

  private Configuration config;

  public MDSApiClientBuilder(Configuration config) {
    this.config = config;
  }

  public MDSApiClient build() {
    String mdsServer = config.getProperty(MDS_SERVER);

    MDSApiClient apiClient = new MDSApiClient(mdsServer);
    // Pass Cluster IDS
    String kafkaClusterID = config.getProperty(MDS_KAFKA_CLUSTER_ID_CONFIG);
    apiClient.setKafkaClusterId(kafkaClusterID);
    String schemaRegistryClusterID = config.getProperty(MDS_SR_CLUSTER_ID_CONFIG);
    apiClient.setSchemaRegistryClusterID(schemaRegistryClusterID);
    String kafkaConnectClusterID = config.getProperty(MDS_KC_CLUSTER_ID_CONFIG);
    apiClient.setConnectClusterID(kafkaConnectClusterID);

    LOGGER.info(String.format("Connecting to an MDS server at %s", mdsServer));
    return apiClient;
  }

  public void configure(Configuration config) {
    this.config = config;
  }
}
