package kafka.ops.topology;

import static kafka.ops.topology.roles.rbac.RBACPredefinedRoles.SECURITY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import kafka.ops.topology.api.mds.ClusterIDs;
import kafka.ops.topology.api.mds.MDSApiClient;
import kafka.ops.topology.api.mds.RequestScope;
import kafka.ops.topology.model.users.Connector;
import kafka.ops.topology.roles.rbac.ClusterLevelRoleBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ClusterLevelRoleBuilderTest {

  @Mock MDSApiClient apiClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private Connector connector;

  private static ClusterIDs allClusterIDs;
  private static ClusterIDs nonClusterIDs;

  @BeforeClass
  public static void beforeClass() {

    allClusterIDs = new ClusterIDs();
    nonClusterIDs = new ClusterIDs();

    allClusterIDs.setConnectClusterID("1234");
    allClusterIDs.setSchemaRegistryClusterID("4321");
    allClusterIDs.setKafkaClusterId("abcd");
  }

  @Before
  public void before() {
    connector = new Connector();
  }

  @Test
  public void testWithAllClientIdsForConnect() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect(connector);

    assertEquals("kafka-connect", runner.getScope().getResource(0).get(RequestScope.RESOURCE_NAME));

    String connectClusterId = "1234";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(ClusterIDs.CONNECT_CLUSTER_ID_LABEL));
  }

  @Test
  public void testWithAllClientIdsForSchemaRegistry() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaRegistry();

    String connectClusterId = "4321";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(ClusterIDs.SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
  }

  @Test
  public void testKafkaRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafka();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(clusterId, clusterIDs.get(ClusterIDs.KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testControlCenterRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forControlCenter();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(clusterId, clusterIDs.get(ClusterIDs.KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testKafkaConnectRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect();

    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals("1234", clusterIDs.get(ClusterIDs.CONNECT_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(ClusterIDs.KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testSchemaRegistryRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIDs);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaSubject("foo");

    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals("4321", clusterIDs.get(ClusterIDs.SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(ClusterIDs.KAFKA_CLUSTER_ID_LABEL));
  }
}
