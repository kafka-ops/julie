package kafka.ops.topology.api.mds;

import static kafka.ops.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static kafka.ops.topology.TopologyBuilderConfig.MDS_KC_CLUSTER_ID_CONFIG;
import static kafka.ops.topology.TopologyBuilderConfig.MDS_SERVER;
import static kafka.ops.topology.TopologyBuilderConfig.MDS_SR_CLUSTER_ID_CONFIG;

import kafka.ops.topology.TopologyBuilderConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MdsApiClientBuilder {

  private static final Logger LOGGER = LogManager.getLogger(MdsApiClientBuilder.class);

  private TopologyBuilderConfig config;

  public MdsApiClientBuilder(TopologyBuilderConfig config) {
    this.config = config;
  }

  public MdsApiClient build() {
    String mdsServer = config.getProperty(MDS_SERVER);

    MdsApiClient apiClient = new MdsApiClient(mdsServer);
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

  public void configure(TopologyBuilderConfig config) {
    this.config = config;
  }
}
