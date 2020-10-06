package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_NAME;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_PATTERN_TYPE;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_TYPE;

import com.purbon.kafka.topology.api.mds.http.HttpDeleteWithBody;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDSApiClient {

  private static final Logger LOGGER = LogManager.getLogger(MDSApiClient.class);

  private final String mdsServer;
  private String basicCredentials;

  private AuthenticationCredentials authenticationCredentials;
  private ClusterIDs clusterIDs;

  public MDSApiClient(String mdsServer) {
    this.mdsServer = mdsServer;
    this.clusterIDs = new ClusterIDs();
  }

  public void login(String user, String password) {
    String userAndPassword = user + ":" + password;
    basicCredentials = Base64.getEncoder().encodeToString(userAndPassword.getBytes());
  }

  public AuthenticationCredentials getCredentials() {
    return authenticationCredentials;
  }

  public void authenticate() throws IOException {
    HttpGet request = new HttpGet(mdsServer + "/security/1.0/authenticate");
    request.addHeader("accept", " application/json");
    request.addHeader("Authorization", "Basic " + basicCredentials);

    Response response;

    try {
      response = get(request);
      if (response.getStatus() < 200 || response.getStatus() > 204) {
        throw new IOException("MDS Authentication error: " + response.getResponseAsString());
      }
      authenticationCredentials =
          new AuthenticationCredentials(
              response.getField("auth_token").toString(),
              response.getField("token_type").toString(),
              Integer.valueOf(response.getField("expires_in").toString()));
    } catch (Exception e) {
      LOGGER.error(e);
      throw new IOException(e);
    }
  }

  public ClusterLevelRoleBuilder bind(String principal, String role) {
    return new ClusterLevelRoleBuilder(principal, role, this);
  }

  public TopologyAclBinding bind(String principal, String role, String topic, String patternType) {
    return bind(principal, role, topic, "Topic", patternType);
  }

  public TopologyAclBinding bind(String principal, String role, RequestScope scope) {

    ResourceType resourceType = ResourceType.fromString(scope.getResource(0).get(RESOURCE_TYPE));
    String resourceName = scope.getResource(0).get(RESOURCE_NAME);
    String patternType = scope.getResource(0).get(RESOURCE_PATTERN_TYPE);

    TopologyAclBinding binding =
        new TopologyAclBinding(resourceType, resourceName, "*", role, principal, patternType);

    binding.setScope(scope);
    return binding;
  }

  public TopologyAclBinding bindClusterRole(String principal, String role, RequestScope scope) {
    ResourceType resourceType = ResourceType.CLUSTER;
    TopologyAclBinding binding =
        new TopologyAclBinding(resourceType, "cluster", "*", role, principal, "LITERAL");
    binding.setScope(scope);
    return binding;
  }

  public void bindRequest(TopologyAclBinding binding) throws IOException {

    String url = binding.getPrincipal() + "/roles/" + binding.getRole();
    if (!binding.getResourceType().equals(ResourceType.CLUSTER)) {
      url = url + "/bindings";
    }

    HttpPost postRequest = buildPostRequest(url);

    try {
      String jsonEntity;
      if (binding.getResourceType().equals(ResourceType.CLUSTER)) {
        jsonEntity = binding.getScope().clustersAsJson();
      } else {
        jsonEntity = binding.getScope().asJson();
      }
      postRequest.setEntity(new StringEntity(jsonEntity));
      LOGGER.debug("bind.entity: " + jsonEntity);
      post(postRequest);
    } catch (IOException e) {
      LOGGER.error(e);
      throw e;
    }
  }

  private HttpPost buildPostRequest(String url) {
    HttpPost postRequest = new HttpPost(mdsServer + "/security/1.0/principals/" + url);
    postRequest.addHeader("accept", " application/json");
    postRequest.addHeader("Content-Type", "application/json");
    postRequest.addHeader("Authorization", "Basic " + basicCredentials);
    return postRequest;
  }

  /**
   * Bind a new RBAC role
   *
   * @param principal
   * @param role
   * @param resource
   * @param resourceType
   * @param patternType
   * @return
   */
  public TopologyAclBinding bind(
      String principal, String role, String resource, String resourceType, String patternType) {

    RequestScope scope = new RequestScope();
    scope.setClusters(clusterIDs.getKafkaClusterIds());
    scope.addResource(resourceType, resource, patternType);
    scope.build();

    return bind(principal, role, scope);
  }

  /**
   * Remove the role (cluster or resource scoped) from the principal at the given scope/cluster.
   * No-op if the user doesn’t have the role. Callable by Admins.
   *
   * @param principal Fully-qualified KafkaPrincipal string for a user or group.
   * @param role The name of the role.
   * @param scope The request scope
   */
  public void deleteRole(String principal, String role, RequestScope scope) {
    HttpDeleteWithBody request =
        new HttpDeleteWithBody(
            mdsServer + "/security/1.0/principals/" + principal + "/roles/" + role);
    request.addHeader("accept", " application/json");
    request.addHeader("Content-Type", "application/json");
    request.addHeader("Authorization", "Basic " + basicCredentials);
    LOGGER.debug("deleteRole: " + request.getURI());
    try {
      request.setEntity(new StringEntity(scope.asJson()));
      LOGGER.debug("bind.entity: " + scope.asJson());
      delete(request);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<String> lookupRoles(String principal) {
    return lookupRoles(principal, clusterIDs.getKafkaClusterIds());
  }

  public List<String> lookupRoles(String principal, Map<String, Map<String, String>> clusters) {
    HttpPost postRequest =
        new HttpPost(mdsServer + "/security/1.0/lookup/principals/" + principal + "/roleNames");
    postRequest.addHeader("accept", " application/json");
    postRequest.addHeader("Content-Type", "application/json");
    postRequest.addHeader("Authorization", "Basic " + basicCredentials);

    List<String> roles = new ArrayList<>();

    try {
      postRequest.setEntity(new StringEntity(JSON.asString(clusters)));
      String stringResponse = post(postRequest);
      if (!stringResponse.isEmpty()) {
        roles = JSON.toArray(stringResponse);
      }
    } catch (IOException e) {
      LOGGER.error(e);
    }

    return roles;
  }

  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  private Response get(HttpGet request) throws IOException {
    LOGGER.debug("GET.request: " + request);
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      LOGGER.debug("GET.response: " + response);
      return new Response(response);
    }
  }

  private String post(HttpPost request) throws IOException {
    LOGGER.debug("POST.request: " + request);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      LOGGER.debug("POST.response: " + response);
      HttpEntity entity = response.getEntity();
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode > 299) {
        throw new IOException(
            "Something happened with the connection, response status code: "
                + statusCode
                + " "
                + request);
      }
      String result = "";
      if (entity != null) {
        result = EntityUtils.toString(entity);
      }
      return result;
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  private String delete(HttpDeleteWithBody request) throws IOException {
    LOGGER.debug("DELETE.request: " + request);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      LOGGER.debug("DELETE.response: " + response);
      HttpEntity entity = response.getEntity();
      // Header headers = entity.getContentType();
      String result = "";
      if (entity != null) {
        result = EntityUtils.toString(entity);
      }

      return result;
    }
  }

  public void setKafkaClusterId(String clusterId) {
    clusterIDs.setKafkaClusterId(clusterId);
  }

  public void setConnectClusterID(String clusterId) {
    clusterIDs.setConnectClusterID(clusterId);
  }

  public void setSchemaRegistryClusterID(String clusterId) {
    clusterIDs.setSchemaRegistryClusterID(clusterId);
  }

  /**
   * Builder method used to compose custom versions of clusterIDs, this is useful when for example
   * listing the permissions using the listResource method.
   *
   * @return ClusterIDs
   */
  public ClusterIDs withClusterIDs() {
    try {
      return clusterIDs.clone().clear();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }
}
