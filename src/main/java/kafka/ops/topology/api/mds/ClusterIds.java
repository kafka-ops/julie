package kafka.ops.topology.api.mds;

import java.util.HashMap;
import java.util.Map;

public class ClusterIds implements Cloneable {

  private String kafkaClusterID;
  private String schemaRegistryClusterID;
  private String connectClusterID;

  public static String KAFKA_CLUSTER_ID_LABEL = "kafka-cluster";
  public static String SCHEMA_REGISTRY_CLUSTER_ID_LABEL = "schema-registry-cluster";
  public static String CONNECT_CLUSTER_ID_LABEL = "connect-cluster";

  private Map<String, String> clusterIds;

  public ClusterIds() {
    this.connectClusterID = "";
    this.kafkaClusterID = "";
    this.schemaRegistryClusterID = "";
    this.clusterIds = new HashMap<>();
  }

  public Map<String, Map<String, String>> getKafkaClusterIds() {
    HashMap<String, String> clusterIds = new HashMap<>();
    if (!kafkaClusterID.isEmpty()) clusterIds.put(KAFKA_CLUSTER_ID_LABEL, kafkaClusterID);

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);
    return clusters;
  }

  public ClusterIds clear() {
    this.clusterIds.clear();
    return this;
  }

  public ClusterIds forKafka() {
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, kafkaClusterID);
    return this;
  }

  public ClusterIds forKafkaConnect() {
    clusterIds.put(CONNECT_CLUSTER_ID_LABEL, connectClusterID);
    return this;
  }

  public ClusterIds forSchemaRegistry() {
    clusterIds.put(SCHEMA_REGISTRY_CLUSTER_ID_LABEL, schemaRegistryClusterID);
    return this;
  }

  public Map<String, Map<String, String>> asMap() {
    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);
    return clusters;
  }

  public void setKafkaClusterId(String kafkaClusterID) {
    this.kafkaClusterID = kafkaClusterID;
  }

  public void setSchemaRegistryClusterID(String schemaRegistryClusterID) {
    this.schemaRegistryClusterID = schemaRegistryClusterID;
  }

  public void setConnectClusterID(String connectClusterID) {
    this.connectClusterID = connectClusterID;
  }

  public ClusterIds clone() throws CloneNotSupportedException {
    return (ClusterIds) super.clone();
  }
}
