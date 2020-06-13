package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.roles.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.RBACProvider.LITERAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.api.mds.AuthenticationCredentials;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.roles.RBACProvider;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class MDSApiClientRbacIT extends MDSBaseTest {

  private static final Logger LOGGER = LogManager.getLogger(MDSApiClientRbacIT.class);

  private String mdsServer = "http://localhost:8090";
  private String mdsUser = "professor";
  private String mdsPassword = "professor";

  private MDSApiClient apiClient;
  private AccessControlManager accessControlManager;

  @Before
  public void before() throws IOException, InterruptedException {
    super.beforeEach();
    apiClient = new MDSApiClient(mdsServer);
    RBACProvider rbacProvider = new RBACProvider(apiClient);
    accessControlManager = new AccessControlManager(rbacProvider);
  }

  @Test
  public void testMDSLogin() {
    apiClient.login(mdsUser, mdsPassword);
    apiClient.authenticate();
    AuthenticationCredentials credentials = apiClient.getCredentials();
    assertEquals(false, credentials.getAuthToken().isEmpty());
  }

  @Test
  public void testWithWrongMDSLogin() {
    apiClient.login("wrong-user", "wrong-password");
    apiClient.authenticate();
    AuthenticationCredentials credentials = apiClient.getCredentials();
    assertEquals(null, credentials);
  }

  @Test
  public void testLookupRoles() {
    apiClient.login(mdsUser, mdsPassword);
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());

    List<String> roles = apiClient.lookupRoles("User:fry");
    assertTrue(roles.contains(DEVELOPER_READ));
  }

  @Test
  public void testBindRoleToResource() {
    apiClient.login(mdsUser, mdsPassword);
    apiClient.authenticate();
    apiClient.setKafkaClusterId(getKafkaClusterID());

    apiClient.bind("User:fry", DEVELOPER_READ, "connect-configs", LITERAL);

    List<String> roles = apiClient.lookupRoles("User:fry");
    assertEquals(1, roles.size());
    assertTrue(roles.contains(DEVELOPER_READ));
  }
}
