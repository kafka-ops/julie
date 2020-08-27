package com.purbon.kafka.topology.roles;

import static com.purbon.kafka.topology.api.mds.ClusterIDs.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIDs.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIDs.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.users.Connector;
import java.io.IOException;
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
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    Map<String, String> clusterIds = clusters.get("clusters");

    validateRequiredClusterLabels(
        clusterIds, KAFKA_CLUSTER_ID_LABEL, SCHEMA_REGISTRY_CLUSTER_ID_LABEL);

    scope = new RequestScope();
    scope.setClusters(clusters);

    scope.build();

    return this;
  }

  public AdminRoleRunner forSchemaSubject(String subject) throws ConfigurationException {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    Map<String, String> clusterIds = clusters.get("clusters");

    validateRequiredClusterLabels(
        clusterIds, KAFKA_CLUSTER_ID_LABEL, SCHEMA_REGISTRY_CLUSTER_ID_LABEL);

    scope = new RequestScope();
    scope.setClusters(clusters);
    String resource = "Subject:" + subject;
    scope.addResource("Subject", resource, PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public AdminRoleRunner forAKafkaConnector(String connector) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

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

    return client.bindClusterRole(principal, role, scope);
  }

  public AdminRoleRunner forKafka() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.build();

    return this;
  }

  public AdminRoleRunner forControlCenter() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "control-center", PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public AdminRoleRunner forKafkaConnect() throws IOException {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    Map<String, String> clusterIds = clusters.get("clusters");
    validateRequiredClusterLabels(clusterIds, KAFKA_CLUSTER_ID_LABEL);

    validateRequiredClusterLabels(clusterIds, KAFKA_CLUSTER_ID_LABEL, CONNECT_CLUSTER_ID_LABEL);

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "kafka-connect", PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public AdminRoleRunner forKafkaConnect(Connector connector) throws IOException {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    Optional<String> connectClusterIdOptional = connector.getCluster_id();
    if (connectClusterIdOptional.isPresent()) {
      clusters.get("clusters").put(CONNECT_CLUSTER_ID_LABEL, connectClusterIdOptional.get());
    }

    validateRequiredClusterLabels(clusters.get("clusters"), KAFKA_CLUSTER_ID_LABEL);
    validateRequiredClusterLabels(
        clusters.get("clusters"), KAFKA_CLUSTER_ID_LABEL, CONNECT_CLUSTER_ID_LABEL);

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
      String value = allClusterIds.get(label);
      if (value == null || value.isEmpty()) {
        throw new ConfigurationException(
            "Required clusterID " + label + " is missing, please add it to your configuration");
      }
    }
  }
}
