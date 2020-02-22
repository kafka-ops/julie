package com.purbon.kafka.topology.integration;

import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.api.mds.AuthenticationCredentials;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.roles.RBACProvider;
import org.junit.Before;
import org.junit.Test;

public class RBACPRoviderRbacIT {


  private String mdsServer = "http://localhost:8090";
  private String mdsUser = "professor";
  private String mdsPassword = "professor";


  private MDSApiClient apiClient;
  private AccessControlManager accessControlManager;

  @Before
  public void before() {
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

}
