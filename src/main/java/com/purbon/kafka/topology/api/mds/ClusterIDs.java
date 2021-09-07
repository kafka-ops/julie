package com.purbon.kafka.topology.api.mds;

import java.util.HashMap;
import java.util.Map;

public class ClusterIDs implements Cloneable {

  private String kafkaClusterID;
  private String schemaRegistryClusterID;
  private String connectClusterID;
  private String ksqlClusterID;

  public static String KAFKA_CLUSTER_ID_LABEL = "kafka-cluster";
  public static String SCHEMA_REGISTRY_CLUSTER_ID_LABEL = "schema-registry-cluster";
  public static String CONNECT_CLUSTER_ID_LABEL = "connect-cluster";
  public static String KSQL_CLUSTER_ID_LABEL = "ksql-cluster";

  private Map<String, String> clusterIds;

  public ClusterIDs() {
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

  public ClusterIDs clear() {
    this.clusterIds.clear();
    return this;
  }

  public ClusterIDs forKafka() {
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, kafkaClusterID);
    return this;
  }

  public ClusterIDs forKafkaConnect() {
    clusterIds.put(CONNECT_CLUSTER_ID_LABEL, connectClusterID);
    return this;
  }

  public ClusterIDs forSchemaRegistry() {
    clusterIds.put(SCHEMA_REGISTRY_CLUSTER_ID_LABEL, schemaRegistryClusterID);
    return this;
  }

  public ClusterIDs forKsql() {
    clusterIds.put(KSQL_CLUSTER_ID_LABEL, ksqlClusterID);
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

  public ClusterIDs clone() {
    ClusterIDs clusterIDs = new ClusterIDs();
    clusterIDs.setKsqlClusterID(ksqlClusterID);
    clusterIDs.setConnectClusterID(connectClusterID);
    clusterIDs.setKafkaClusterId(kafkaClusterID);
    clusterIDs.setSchemaRegistryClusterID(schemaRegistryClusterID);
    return clusterIDs;
  }

  public void setKsqlClusterID(String clusterId) {
    this.ksqlClusterID = clusterId;
  }
}
