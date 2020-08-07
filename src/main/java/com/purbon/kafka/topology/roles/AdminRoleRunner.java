package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.api.mds.MDSApiClient.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.users.Connector;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AdminRoleRunner {

  private final String principal;
  private final String role;
  private final MDSApiClient client;
  private Map<String, Object> scope;
  private String resourceName;

  public AdminRoleRunner(String principal, String role, MDSApiClient client) {
    this.principal = principal;
    this.role = role;
    this.client = client;
    this.scope = new HashMap<>();
    this.resourceName = "";
  }

  public AdminRoleRunner forSchemaRegistry() throws ConfigurationException {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    validateRequiredClusterLabels(
        allClusterIds, KAFKA_CLUSTER_ID_LABEL, SCHEMA_REGISTRY_CLUSTER_ID_LABEL);
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(
        SCHEMA_REGISTRY_CLUSTER_ID_LABEL, allClusterIds.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));

    scope.clear();
    scope.put("clusters", clusterIds);
    this.resourceName = "schema-registry";
    return this;
  }

  public AdminRoleRunner forSchemaSubject(String subject) throws ConfigurationException {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    validateRequiredClusterLabels(
        allClusterIds, KAFKA_CLUSTER_ID_LABEL, SCHEMA_REGISTRY_CLUSTER_ID_LABEL);
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(
        SCHEMA_REGISTRY_CLUSTER_ID_LABEL, allClusterIds.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));

    scope.clear();
    scope.put("clusters", clusterIds);
    this.resourceName = "Subject:" + subject;
    return this;
  }

  public TopologyAclBinding apply() {
    return client.bindRole(principal, role, resourceName, scope);
  }

  public AdminRoleRunner forControlCenter() {
    scope.clear();
    client.getKafkaClusterIds().forEach((key, value) -> scope.put(key, value));

    this.resourceName = "control-center";
    return this;
  }

  public AdminRoleRunner forKafkaConnect(Connector connector) throws IOException {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    validateRequiredClusterLabels(allClusterIds, KAFKA_CLUSTER_ID_LABEL);

    Optional<String> connectClusterIdOptional = connector.getCluster_id();
    validateRequiredClusterLabels(allClusterIds, KAFKA_CLUSTER_ID_LABEL, CONNECT_CLUSTER_ID_LABEL);

    String connectClusterID =
        connectClusterIdOptional.orElse(allClusterIds.get(CONNECT_CLUSTER_ID_LABEL));

    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(CONNECT_CLUSTER_ID_LABEL, connectClusterID);

    scope.clear();
    scope.put("clusters", clusterIds);

    this.resourceName = "kafka-connect";
    return this;
  }

  public Map<String, Object> getScope() {
    return scope;
  }

  public String getResourceName() {
    return resourceName;
  }

  private void validateRequiredClusterLabels(
      Map<String, String> allClusterIds, String... clusterLabels) throws ConfigurationException {
    for (String label : clusterLabels) {
      if (allClusterIds.get(label) == null) {
        throw new ConfigurationException(
            "Required clusterID " + label + " is missing, please add it to your configuration");
      }
    }
  }
}
