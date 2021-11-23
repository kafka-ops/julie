package com.purbon.kafka.topology.roles.rbac;

import static com.purbon.kafka.topology.api.mds.ClusterIDs.CONNECT_CLUSTER_ID_LABEL;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.RequestScope;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.resource.PatternType;

public class ClusterLevelRoleBuilder {

  private final String principal;
  private final String role;
  private final MDSApiClient client;
  private RequestScope scope;

  public ClusterLevelRoleBuilder(String principal, String role, MDSApiClient client) {
    this.principal = principal;
    this.role = role;
    this.client = client;
    this.scope = new RequestScope();
  }

  public ClusterLevelRoleBuilder forSchemaRegistry() {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forSchemaSubject(String subject) {
    return forSchemaSubject(subject, PatternType.LITERAL.name());
  }

  public ClusterLevelRoleBuilder forSchemaSubject(String subject, String patternType) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forSchemaRegistry().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Subject", subject, patternType);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forAKafkaConnector(String connector) {
    return forAKafkaConnector(connector, PatternType.LITERAL.name());
  }

  public ClusterLevelRoleBuilder forAKafkaConnector(String connector, String patternType) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Connector", connector, patternType);
    scope.build();

    return this;
  }

  public TopologyAclBinding apply() {
    return apply("CLUSTER", "cluster");
  }

  public TopologyAclBinding apply(String resourceType, String resourceName) {
    return apply(resourceType, resourceName, "LITERAL");
  }

  public TopologyAclBinding apply(String resourceType, String resourceName, String patternType) {
    return client.bindClusterRole(principal, resourceType, resourceName, role, scope, patternType);
  }

  public ClusterLevelRoleBuilder forKafka() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forControlCenter() {
    Map<String, Map<String, String>> clusters = client.withClusterIDs().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "control-center", PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKSqlServer(String clusterId) {
    client.setKSqlClusterID(clusterId);
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKsql().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("KsqlCluster", "ksql-cluster", PatternType.LITERAL.name());
    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKafkaConnect() {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "kafka-connect", PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public ClusterLevelRoleBuilder forKafkaConnect(Connector connector) {
    Map<String, Map<String, String>> clusters =
        client.withClusterIDs().forKafkaConnect().forKafka().asMap();

    Optional<String> connectClusterIdOptional = connector.getCluster_id();
    connectClusterIdOptional.ifPresent(
        s -> clusters.get("clusters").put(CONNECT_CLUSTER_ID_LABEL, s));

    scope = new RequestScope();
    scope.setClusters(clusters);
    scope.addResource("Cluster", "kafka-connect", PatternType.LITERAL.name());

    scope.build();

    return this;
  }

  public RequestScope getScope() {
    return scope;
  }
}
