package com.purbon.kafka.topology.roles;

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

  public AdminRoleRunner(String principal, String role, MDSApiClient client) {
    this.principal = principal;
    this.role = role;
    this.client = client;
  }

  public AdminRoleRunner forSchemaRegistry() {
    Map<String, String> clusterIds = new HashMap<>();
    clusterIds.put(
        KAFKA_CLUSTER_ID_LABEL, client.getClusterIds().get("clusters").get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(
        SCHEMA_REGISTRY_CLUSTER_ID_LABEL,
        client.getClusterIds().get("clusters").get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);
    scope = client.buildResourceScope("ALL", "Cluster", "LITERAL", clusters);
    return this;
  }

  public void apply() {
    client.bind(principal, role, scope);
  }
}
