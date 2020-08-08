package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.api.mds.MDSApiClient.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.users.Connector;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.resource.PatternType;

public class AdminRoleRunner {

  private final String principal;
  private final String role;
  private final MDSApiClient client;
  private RequestScope scope;

  public AdminRoleRunner(String principal, String role, MDSApiClient client) {
    this.principal = principal;
    this.role = role;
    this.client = client;
    this.scope = new RequestScope();
  }

  public AdminRoleRunner forSchemaRegistry() throws ConfigurationException {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    validateRequiredClusterLabels(
        allClusterIds, KAFKA_CLUSTER_ID_LABEL, SCHEMA_REGISTRY_CLUSTER_ID_LABEL);
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(
        SCHEMA_REGISTRY_CLUSTER_ID_LABEL, allClusterIds.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "schema-registry", PatternType.LITERAL.name());

    scope.build();

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

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);

    scope = new RequestScope();
    scope.setClusters(clusters);
    String resource = "Subject:" + subject;
    scope.addResource("Subject", resource, PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public AdminRoleRunner forAKafkaConnector(String connector) {
    Map<String, String> clusterIds = new HashMap<>();
    Map<String, String> allClusterIds = client.getClusterIds().get("clusters");
    clusterIds.put(KAFKA_CLUSTER_ID_LABEL, allClusterIds.get(KAFKA_CLUSTER_ID_LABEL));
    clusterIds.put(CONNECT_CLUSTER_ID_LABEL, allClusterIds.get(CONNECT_CLUSTER_ID_LABEL));

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);

    String resource = "Connector:" + connector;
    String resourceType = "Connector";
    String patternType = PatternType.LITERAL.name();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource(resourceType, resource, patternType);
    scope.build();

    return this;
  }

  public TopologyAclBinding apply() {

    return client.bind(principal, role, scope);
  }

  public AdminRoleRunner forControlCenter() {
    Map<String, String> clusterIds = new HashMap<>();

    client
        .getKafkaClusterIds()
        .forEach((key, ids) -> ids.forEach((k1, v1) -> clusterIds.put(k1, v1)));

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "control-center", PatternType.LITERAL.name());
    scope.build();

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

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "kafka-connect", PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public RequestScope getScope() {
    return scope;
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
