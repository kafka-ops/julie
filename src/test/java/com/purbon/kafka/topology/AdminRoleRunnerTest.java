package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.api.mds.MDSApiClient.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.MDSApiClient.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.SECURITY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.AdminRoleRunner;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AdminRoleRunnerTest {

  @Mock MDSApiClient apiClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static Map<String, Map<String, String>> allClusterIds;
  private static Map<String, Map<String, String>> noClusterIds;
  private Connector connector;

  @BeforeClass
  public static void beforeClass() {

    Map<String, String> allIds = new HashMap<>();

    allIds.put(CONNECT_CLUSTER_ID_LABEL, "1234");
    allIds.put(SCHEMA_REGISTRY_CLUSTER_ID_LABEL, "4321");
    allIds.put(KAFKA_CLUSTER_ID_LABEL, "abcd");

    allClusterIds = Collections.singletonMap("clusters", allIds);
    noClusterIds = Collections.singletonMap("clusters", new HashMap<>());
  }

  @Before
  public void before() {
    connector = new Connector();
  }

  @Test
  public void testWithAllClientIdsForConnect() throws IOException {

    when(apiClient.getClusterIds()).thenReturn(allClusterIds);

    AdminRoleRunner runner = new AdminRoleRunner("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect(connector);

    assertEquals("kafka-connect", runner.getResourceName());

    String connectClusterId = "1234";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(CONNECT_CLUSTER_ID_LABEL));
  }

  @Test(expected = ConfigurationException.class)
  public void testWithMissingClientIdsForConnect() throws IOException {

    when(apiClient.getClusterIds()).thenReturn(noClusterIds);

    AdminRoleRunner runner = new AdminRoleRunner("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect(connector);
  }

  @Test
  public void testWithAllClientIdsForSchemaRegistry() throws IOException {

    when(apiClient.getClusterIds()).thenReturn(allClusterIds);

    AdminRoleRunner runner = new AdminRoleRunner("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaRegistry();

    assertEquals("schema-registry", runner.getResourceName());

    String connectClusterId = "4321";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
  }

  @Test(expected = ConfigurationException.class)
  public void testWithMissingClientIdsForSchemaRegistry() throws IOException {

    when(apiClient.getClusterIds()).thenReturn(noClusterIds);

    AdminRoleRunner runner = new AdminRoleRunner("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaRegistry();
  }
}
