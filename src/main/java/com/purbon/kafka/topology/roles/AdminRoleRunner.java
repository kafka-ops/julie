package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.api.mds.MDSApiClient.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import java.util.HashMap;
import java.util.Map;

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

  public AdminRoleRunner forSchemaRegistry() {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(
        SCHEMA_REGISTRY_CLUSTER_ID_LABEL, allClusterIds.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));

    scope.clear();
    scope.put("clusters", clusterIds);
    this.resourceName = "schema-registry";
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

  public AdminRoleRunner forKafkaConnect() {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(CONNECT_CLUSTER_ID_LABEL, allClusterIds.get(CONNECT_CLUSTER_ID_LABEL));

    scope.clear();
    scope.put("clusters", clusterIds);

    this.resourceName = "kafka-connect";
    return this;
  }
}
