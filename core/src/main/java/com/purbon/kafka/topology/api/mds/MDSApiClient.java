package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_NAME;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_PATTERN_TYPE;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_TYPE;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.isClusterScopedRole;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.SneakyThrows;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDSApiClient extends JulieHttpClient {

  private static final Logger LOGGER = LogManager.getLogger(MDSApiClient.class);

  private AuthenticationCredentials authenticationCredentials;
  private final ClusterIDs clusterIDs;

  public MDSApiClient(String mdsServer) throws IOException {
    this(mdsServer, Optional.empty());
  }

  public MDSApiClient(String mdsServer, Optional<Configuration> configOptional) throws IOException {
    super(mdsServer, configOptional);
    this.clusterIDs = new ClusterIDs(configOptional);
  }

  @Override
  protected HttpClient configureHttpOrHttpsClient(Optional<Configuration> configOptional)
      throws IOException {
    if (configOptional.isEmpty()) {
      return HttpClient.newBuilder().build();
    }

    Configuration config = configOptional.get();

    if (!config.mdsInsecureAllowed()) {
      return super.configureHttpOrHttpsClient(configOptional);
    } else {
      return trustAllClient();
    }
  }

  @SneakyThrows
  private HttpClient trustAllClient() {
    LOGGER.info("MDS running with trust all connections");
    final Properties props = System.getProperties();
    props.setProperty(
        "jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

    var trustManagers =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}
          }
        };
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagers, new SecureRandom());

    return HttpClient.newBuilder().sslContext(sslContext).build();
  }

  public AuthenticationCredentials getCredentials() {
    return authenticationCredentials;
  }

  public void authenticate() throws IOException {
    try {
      Response response = doGet("/security/1.0/authenticate");
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

  public TopologyAclBinding bindClusterRole(String principal, String role, RequestScope scope) {
    return bindClusterRole(principal, ResourceType.CLUSTER.name(), "cluster", role, scope);
  }

  public TopologyAclBinding bindClusterRole(
      String principal, String resourceType, String resourceName, String role, RequestScope scope) {
    return bindClusterRole(principal, resourceType, resourceName, role, scope, "LITERAL");
  }

  public TopologyAclBinding bindClusterRole(
      String principal,
      String resourceType,
      String resourceName,
      String role,
      RequestScope scope,
      String patternType) {
    TopologyAclBinding binding =
        new TopologyAclBinding(resourceType, resourceName, "*", role, principal, patternType);
    binding.setScope(scope);
    return binding;
  }

  private boolean isBindingWithResources(TopologyAclBinding binding) {
    return !binding.getScope().getResources().isEmpty();
  }

  MDSRequest buildRequest(TopologyAclBinding binding) {
    String url = binding.getPrincipal() + "/roles/" + binding.getOperation();
    String jsonEntity;

    if (isBindingWithResources(binding) && !isClusterScopedRole(binding.getOperation())) {
      url = url + "/bindings";
      jsonEntity = binding.getScope().asJson();
    } else {
      jsonEntity = binding.getScope().clustersAsJson();
    }
    LOGGER.debug("bind.entity: " + jsonEntity);

    return new MDSRequest(url, jsonEntity);
  }

  public void bindRequest(TopologyAclBinding binding) throws IOException {
    MDSRequest mdsRequest = buildRequest(binding);
    try {
      LOGGER.debug("bind.entity: " + mdsRequest.getJsonEntity());
      doPost("/security/1.0/principals/" + mdsRequest.getUrl(), mdsRequest.getJsonEntity());
    } catch (IOException e) {
      LOGGER.error(e);
      throw e;
    }
  }

  /**
   * Create an RBAC resource binding
   *
   * @param principal
   * @param role
   * @param resource
   * @param resourceType
   * @param patternType
   * @return TopologyAclBinding
   */
  public TopologyAclBinding bind(
      String principal, String role, String resource, String resourceType, String patternType) {

    RequestScope scope = new RequestScope();
    scope.setClusters(clusterIDs.getKafkaClusterIds());
    scope.addResource(resourceType, resource, patternType);
    scope.build();

    return bind(principal, role, scope);
  }

  private TopologyAclBinding bind(String principal, String role, RequestScope scope) {

    ResourceType resourceType = ResourceType.fromString(scope.getResource(0).get(RESOURCE_TYPE));
    String resourceName = scope.getResource(0).get(RESOURCE_NAME);
    String patternType = scope.getResource(0).get(RESOURCE_PATTERN_TYPE);

    TopologyAclBinding binding =
        new TopologyAclBinding(
            resourceType.name(), resourceName, "*", role, principal, patternType);

    binding.setScope(scope);
    return binding;
  }

  /**
   * Remove the role (cluster or resource scoped) from the principal at the given scope/cluster.
   * No-op if the user doesn't have the role. Callable by Admins.
   *
   * @param principal Fully-qualified KafkaPrincipal string for a user or group.
   * @param role The name of the role.
   * @param scope The request scope
   */
  public void deleteRole(String principal, String role, RequestScope scope) {
    String url = "/security/1.0/principals/" + principal + "/roles/" + role + "/bindings";
    try {
      doDelete(url, scope.asJson());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<String> lookupKafkaPrincipalsByRoleForKafka(String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().asMap();
    return lookupKafkaPrincipalsByRole(role, clusters);
  }

  public List<String> lookupKafkaPrincipalsByRoleForConnect(String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().forKafkaConnect().asMap();
    return lookupKafkaPrincipalsByRole(role, clusters);
  }

  public List<String> lookupKafkaPrincipalsByRoleForSchemaRegistry(String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().forSchemaRegistry().asMap();
    return lookupKafkaPrincipalsByRole(role, clusters);
  }

  public List<String> lookupKafkaPrincipalsByRole(
      String role, Map<String, Map<String, String>> clusters) {
    List<String> users = new ArrayList<>();

    try {
      String url = "/security/1.0/lookup/role/" + role;
      String response = doPost(url, JSON.asString(clusters));
      if (!response.isEmpty()) {
        users = JSON.toArray(response);
      }
    } catch (IOException ex) {
      LOGGER.error(ex);
    }
    return users;
  }

  public List<String> lookupRoles(String principal) {
    return lookupRoles(principal, clusterIDs.getKafkaClusterIds());
  }

  public List<String> lookupRoles(String principal, Map<String, Map<String, String>> clusters) {
    List<String> roles = new ArrayList<>();
    try {
      String url = "/security/1.0/lookup/principals/" + principal + "/roleNames";
      String response = doPost(url, JSON.asString(clusters));
      if (!response.isEmpty()) {
        roles = JSON.toArray(response);
      }
    } catch (IOException e) {
      LOGGER.error(e);
    }

    return roles;
  }

  public List<RbacResourceType> lookupResourcesForKafka(String principal, String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().asMap();
    return lookupResources(principal, role, clusters);
  }

  public List<RbacResourceType> lookupResourcesForConnect(String principal, String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().forKafkaConnect().asMap();
    return lookupResources(principal, role, clusters);
  }

  public List<RbacResourceType> lookupResourcesForSchemaRegistry(String principal, String role) {
    Map<String, Map<String, String>> clusters = clusterIDs.forKafka().forSchemaRegistry().asMap();
    return lookupResources(principal, role, clusters);
  }

  public List<RbacResourceType> lookupResources(
      String principal, String role, Map<String, Map<String, String>> clusters) {
    List<RbacResourceType> resources = new ArrayList<>();
    try {
      String url = "/security/1.0/principals/" + principal + "/roles/" + role + "/resources";
      String response = doPost(url, JSON.asString(clusters));
      if (!response.isEmpty()) {
        resources = (List<RbacResourceType>) JSON.toObjectList(response, RbacResourceType.class);
      }
    } catch (IOException e) {
      LOGGER.error(e);
    }

    return resources;
  }

  public List<String> getRoleNames() {
    List<String> roles = new ArrayList<>();
    try {
      String url = "/security/1.0/roleNames";
      Response response = doGet(url);
      String[] myRoles = (String[]) JSON.toObject(response.getResponseAsString(), String[].class);
      roles = Arrays.asList(myRoles);
    } catch (IOException e) {
      LOGGER.error(e);
    }
    return roles;
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

  public void setKSqlClusterID(String clusterId) {
    clusterIDs.setKsqlClusterID(clusterId);
  }

  /**
   * Builder method used to compose custom versions of clusterIDs, this is useful when for example
   * listing the permissions using the listResource method.
   *
   * @return ClusterIDs
   */
  public ClusterIDs withClusterIDs() {
    return clusterIDs.clone().clear();
  }
}
