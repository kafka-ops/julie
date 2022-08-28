package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder.LITERAL;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.RESOURCE_OWNER;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.SECURITY_ADMIN;
import static org.junit.jupiter.api.Assertions.*;

import com.purbon.kafka.topology.api.mds.AuthenticationCredentials;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.BasicAuth;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MDSApiClientRbacIT extends MDSBaseTest {

  private final String mdsUser = "professor";
  private final String mdsPassword = "professor";

  private MDSApiClient apiClient;

  @BeforeEach
  public void before() throws IOException, InterruptedException {
    super.beforeEach();
    String mdsServer = "http://localhost:8090";
    apiClient = new MDSApiClient(mdsServer);
  }

  @Test
  void mDSLogin() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    AuthenticationCredentials credentials = apiClient.getCredentials();
    assertFalse(credentials.getAuthToken().isEmpty());
  }

  @Test
  void withWrongMDSLogin() throws IOException {
    assertThrows(
        IOException.class,
        () -> {
          apiClient.setBasicAuth(new BasicAuth("wrong-user", "wrong-password"));
          apiClient.authenticate();
        });
  }

  @Test
  void lookupRoles() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());

    List<String> roles = apiClient.lookupRoles("User:fry");
    assertTrue(roles.contains(DEVELOPER_READ));
  }

  @Test
  void bindRoleToResource() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());

    TopologyAclBinding binding =
        apiClient.bind("User:fry", DEVELOPER_READ, "connect-configs", LITERAL);

    apiClient.bindRequest(binding);

    List<String> roles = apiClient.lookupRoles("User:fry");
    assertEquals(1, roles.size());
    assertTrue(roles.contains(DEVELOPER_READ));
  }

  @Test
  void bindRoleWithoutAuthentication() throws IOException {
    assertThrows(
        IOException.class,
        () -> {
          apiClient.setKafkaClusterId(getKafkaClusterID());

          TopologyAclBinding binding =
              apiClient.bind("User:fry", DEVELOPER_READ, "connect-configs", LITERAL);

          apiClient.bindRequest(binding);
        });
  }

  @Test
  void bindSecurityAdminRole() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());
    apiClient.setSchemaRegistryClusterID("schema-registry");
    String principal = "User:foo" + System.currentTimeMillis();

    TopologyAclBinding binding =
        apiClient.bind(principal, SECURITY_ADMIN).forSchemaRegistry().apply();

    apiClient.bindRequest(binding);

    Map<String, Map<String, String>> clusters =
        apiClient.withClusterIDs().forKafka().forSchemaRegistry().asMap();

    List<String> roles = apiClient.lookupRoles(principal, clusters);
    assertEquals(1, roles.size());
    assertTrue(roles.contains(SECURITY_ADMIN));
  }

  @Test
  void bindSubjectRole() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());
    apiClient.setSchemaRegistryClusterID("schema-registry");

    String principal = "User:foo" + System.currentTimeMillis();
    String subject = "topic-value";

    TopologyAclBinding binding =
        apiClient
            .bind(principal, DEVELOPER_READ)
            .forSchemaSubject(subject)
            .apply("Subject", subject);

    apiClient.bindRequest(binding);

    Map<String, Map<String, String>> clusters =
        apiClient.withClusterIDs().forKafka().forSchemaRegistry().asMap();

    List<String> roles = apiClient.lookupRoles(principal, clusters);
    assertEquals(1, roles.size());
    assertTrue(roles.contains(DEVELOPER_READ));
  }

  @Test
  void bindResourceOwnerRole() throws IOException {
    apiClient.setBasicAuth(new BasicAuth(mdsUser, mdsPassword));
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());

    String principal = "User:fry" + System.currentTimeMillis();
    TopologyAclBinding binding =
        apiClient.bind(principal, RESOURCE_OWNER, "connect-configs", LITERAL);
    apiClient.bindRequest(binding);

    List<String> roles = apiClient.lookupRoles(principal);
    assertEquals(1, roles.size());
    assertTrue(roles.contains(RESOURCE_OWNER));
  }
}
