package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KC_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SR_CLUSTER_ID_CONFIG;

import com.purbon.kafka.topology.TopologyBuilderConfig;

public class MDSApiClientBuilder {

  private TopologyBuilderConfig config;

  public MDSApiClientBuilder(TopologyBuilderConfig config) {
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

    return apiClient;
  }

  public void configure(TopologyBuilderConfig config) {
    this.config = config;
  }
}
