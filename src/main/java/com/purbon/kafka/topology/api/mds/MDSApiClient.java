package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_NAME;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_PATTERN_TYPE;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_TYPE;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(mdsServer + "/security/1.0/authenticate"))
            .timeout(Duration.ofMinutes(1))
            .header("accept", " application/json")
            .header("Authorization", "Basic " + basicCredentials)
            .GET()
            .build();

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

  private TopologyAclBinding bind(String principal, String role, RequestScope scope) {

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

    try {
      String jsonEntity;
      if (binding.getResourceType().equals(ResourceType.CLUSTER)) {
        jsonEntity = binding.getScope().clustersAsJson();
      } else {
        jsonEntity = binding.getScope().asJson();
      }
      LOGGER.debug("bind.entity: " + jsonEntity);
      HttpRequest postRequest = buildPostRequest(url, jsonEntity);
      post(postRequest);
    } catch (IOException e) {
      LOGGER.error(e);
      throw e;
    }
  }

  private HttpRequest buildPostRequest(String url, String body) {
    return HttpRequest.newBuilder()
        .uri(URI.create(mdsServer + "/security/1.0/principals/" + url))
        .timeout(Duration.ofMinutes(1))
        .header("accept", " application/json")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic " + basicCredentials)
        .POST(BodyPublishers.ofString(body))
        .build();
  }

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
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(mdsServer + "/security/1.0/principals/" + principal + "/roles/" + role))
            .header("accept", " application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + basicCredentials)
            .method("DELETE", BodyPublishers.ofString(scope.asJson()))
            .build();

    LOGGER.debug("deleteRole: " + request.uri());

    try {
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

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    mdsServer + "/security/1.0/lookup/principals/" + principal + "/roleNames"))
            .timeout(Duration.ofMinutes(1))
            .header("accept", " application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + basicCredentials);

    List<String> roles = new ArrayList<>();

    try {
      requestBuilder.POST(BodyPublishers.ofString(JSON.asString(clusters)));
      String response = post(requestBuilder.build());
      if (!response.isEmpty()) {
        roles = JSON.toArray(response);
      }
    } catch (IOException e) {
      LOGGER.error(e);
    }

    return roles;
  }

  private final HttpClient httpClient = HttpClient.newBuilder().build();

  private Response get(HttpRequest request) throws IOException {
    LOGGER.debug("GET.request: " + request);
    try {
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      LOGGER.debug("GET.response: " + response);
      return new Response(response);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private String post(HttpRequest request) throws IOException {
    LOGGER.debug("POST.request: " + request);
    String result = "";
    try {
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      LOGGER.debug("POST.response: " + response);
      int statusCode = response.statusCode();
      if (statusCode < 200 || statusCode > 299) {
        throw new IOException(
            "Something happened with the connection, response status code: "
                + statusCode
                + " "
                + request);
      }

      if (response.body() != null) {
        result = response.body();
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return result;
  }

  private void delete(HttpRequest request) throws IOException {
    LOGGER.debug("DELETE.request: " + request);
    String result = "";
    try {
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.body() != null) {
        result = response.body();
      }
      LOGGER.debug("DELETE.response: " + response + " result: " + result);
    } catch (Exception ex) {
      throw new IOException(ex);
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
