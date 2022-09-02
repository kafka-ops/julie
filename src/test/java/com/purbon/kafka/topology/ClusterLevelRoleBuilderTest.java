package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.api.mds.ClusterIDs.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIDs.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIDs.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_NAME;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.SECURITY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.api.mds.ClusterIDs;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClusterLevelRoleBuilderTest {

  @Mock MDSApiClient apiClient;

  private Connector connector;

  private static ClusterIDs allClusterIDs;
  private static ClusterIDs nonClusterIDs;

  @BeforeAll
  public static void beforeClass() {

    allClusterIDs = new ClusterIDs();
    nonClusterIDs = new ClusterIDs();

    allClusterIDs.setConnectClusterID("1234");
    allClusterIDs.setSchemaRegistryClusterID("4321");
    allClusterIDs.setKafkaClusterId("abcd");
  }

  @BeforeEach
  void before() {
    connector = new Connector();
  }

  @Test
  void withAllClientIdsForConnect() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect(connector);

    assertEquals("kafka-connect", runner.getScope().getResource(0).get(RESOURCE_NAME));

    String connectClusterId = "1234";
    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(CONNECT_CLUSTER_ID_LABEL));
  }

  @Test
  void withAllClientIdsForSchemaRegistry() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaRegistry();

    String connectClusterId = "4321";
    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
  }

  @Test
  void kafkaRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafka();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals(clusterId, clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  void controlCenterRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forControlCenter();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals(clusterId, clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  void kafkaConnectRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect();

    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals("1234", clusterIDs.get(CONNECT_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  void schemaRegistryRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaSubject("foo");

    Map<String, String> clusterIDs = runner.getScope().clusterIDs();
    assertEquals("4321", clusterIDs.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }
}
