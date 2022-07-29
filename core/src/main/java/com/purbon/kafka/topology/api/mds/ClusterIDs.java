package com.purbon.kafka.topology.api.mds;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;

public class ClusterIDs implements Cloneable {

  private String kafkaClusterID;
  private String schemaRegistryClusterID;
  private String connectClusterID;
  private String ksqlClusterID;
  private List<String> validClusterIds;

  public static String KAFKA_CLUSTER_ID_LABEL = "kafka-cluster";
  public static String SCHEMA_REGISTRY_CLUSTER_ID_LABEL = "schema-registry-cluster";
  public static String CONNECT_CLUSTER_ID_LABEL = "connect-cluster";
  public static String KSQL_CLUSTER_ID_LABEL = "ksql-cluster";

  private Map<String, String> clusterIds;

  public ClusterIDs() {
    this(Optional.empty());
  }

  public ClusterIDs(Optional<Configuration> configOptional) {
    this.connectClusterID = "";
    this.kafkaClusterID = "";
    this.schemaRegistryClusterID = "";
    this.clusterIds = new HashMap<>();
    this.validClusterIds = new ArrayList<>();
    configOptional.ifPresent(config -> validClusterIds = config.getValidClusterIds());
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

  @SneakyThrows
  public void setKafkaClusterId(String clusterId) {
    if (!isItValidId(clusterId)) {
      throw new ValidationException(
          "Kafka clusterId: " + clusterId + " is it not valid. Check your config!");
    }
    this.kafkaClusterID = clusterId;
  }

  @SneakyThrows
  public void setSchemaRegistryClusterID(String clusterId) {
    if (!isItValidId(clusterId)) {
      throw new ValidationException(
          "Schema Registry clusterId: " + clusterId + " is it not valid. Check your config!");
    }
    this.schemaRegistryClusterID = clusterId;
  }

  @SneakyThrows
  public void setConnectClusterID(String clusterId) {
    if (!isItValidId(clusterId)) {
      throw new ValidationException(
          "Kafka Connect clusterId: " + clusterId + " is it not valid. Check your config!");
    }
    this.connectClusterID = clusterId;
  }

  public ClusterIDs clone() {
    ClusterIDs clusterIDs = new ClusterIDs();
    clusterIDs.setKsqlClusterID(ksqlClusterID);
    clusterIDs.setConnectClusterID(connectClusterID);
    clusterIDs.setKafkaClusterId(kafkaClusterID);
    clusterIDs.setSchemaRegistryClusterID(schemaRegistryClusterID);
    return clusterIDs;
  }

  @SneakyThrows
  public void setKsqlClusterID(String clusterId) {
    if (!isItValidId(clusterId)) {
      throw new ValidationException(
          "ksqlClusterId: " + clusterId + " is it not valid. Check your config!");
    }
    this.ksqlClusterID = clusterId;
  }

  private boolean isItValidId(String clusterId) {
    return validClusterIds.isEmpty() || validClusterIds.contains(clusterId);
  }
}
