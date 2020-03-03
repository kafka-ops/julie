package com.purbon.kafka.topology.api.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.Map;

public class MDSApiClient {

  private final String mdsServer;
  private String basicCredentials;

  private AuthenticationCredentials authenticationCredentials;
  private String kafkaClusterID;

  public MDSApiClient(String mdsServer) {
    this.mdsServer = mdsServer;
    this.kafkaClusterID = "";
  }

  public void login(String user, String password) {
    String userAndPassword = user + ":" + password;
    basicCredentials = Base64.getEncoder().encodeToString(userAndPassword.getBytes());
  }

  public AuthenticationCredentials getCredentials() {
    return authenticationCredentials;
  }

  public void authenticate() {
    HttpGet request = new HttpGet(mdsServer + "/security/1.0/authenticate");
    request.addHeader("accept", " application/json");
    request.addHeader("Authorization", "Basic "+ basicCredentials);

    try {
      String responseAsString = get(request);
      if (!responseAsString.isEmpty()) {
        Map<String, Object> responseMap = JSON.toMap(responseAsString);

        authenticationCredentials = new AuthenticationCredentials(
            responseMap.get("auth_token").toString(),
            responseMap.get("token_type").toString(),
            Integer.valueOf(responseMap.get("expires_in").toString())
        );
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void bind(String principal, String role, String topic, String patternType) {
   bind(principal, role, topic, "Topic", patternType);
  }

  public void bind(String principal, String role, String resource, String resourceType, String patternType) {

    HttpPost postRequest = new HttpPost(mdsServer + "/security/1.0/principals/"+principal+"/roles/"+role+"/bindings");
    postRequest.addHeader("accept", " application/json");
    postRequest.addHeader("Content-Type", "application/json");
    postRequest.addHeader("Authorization", "Basic "+ basicCredentials);

    try {
      Map<String, Object> scope = buildResourceScope(resourceType, resource, patternType);
      postRequest.setEntity(new StringEntity(JSON.asString(scope)));
      post(postRequest);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Map<String, Object> buildResourceScope(String resourceType, String name, String patternType) {

    Map<String, Map<String, String>> clusters = getClusterIds();

    Map<String, String> resource = new HashMap<>();
    resource.put("resourceType", resourceType);
    resource.put("name", name);
    resource.put("patternType", patternType);

    List<Map<String, String>> resourcePatterns = new ArrayList<>();
    resourcePatterns.add(resource);

    Map<String, Object> scope = new HashMap<>();
    scope.put("scope", clusters );
    scope.put("resourcePatterns", resourcePatterns);

    return scope;
  }

  public List<String> lookupRoles(String principal) {
    HttpPost postRequest = new HttpPost(mdsServer + "/security/1.0/lookup/principals/"+principal+"/roleNames");
    postRequest.addHeader("accept", " application/json");
    postRequest.addHeader("Content-Type", "application/json");
    postRequest.addHeader("Authorization", "Basic "+ basicCredentials);


    List<String> roles = new ArrayList<>();

    try {
      postRequest.setEntity(new StringEntity(JSON.asString(getClusterIds())));
      String stringResponse = post(postRequest);
      if (!stringResponse.isEmpty()) {
        roles = JSON.toArray(stringResponse);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return roles;
  }

  private Map<String,Map<String, String>> getClusterIds() {
    HashMap<String, String> clusterIds = new HashMap<>();
    if (!kafkaClusterID.isEmpty())
      clusterIds.put("kafka-cluster", kafkaClusterID);
    //clusterIds.put("connect-cluster", "connect-cluster");
    //clusterIds.put("ksql-cluster", "ksqlCluster");
    //clusterIds.put("schema-registry-cluster", "schemaRegistryClusterId");

    Map<String, Map<String, String>> clusters = new HashMap<>();
    clusters.put("clusters", clusterIds);
    return clusters;
  }

  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  private String get(HttpGet request) throws IOException {

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      HttpEntity entity = response.getEntity();
      //Header headers = entity.getContentType();
      String result = "";
      if (entity != null) {
        result = EntityUtils.toString(entity);
      }

      return result;
    }

  }

  private String post(HttpPost request) throws IOException {

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      System.out.println(response);
      HttpEntity entity = response.getEntity();
      //Header headers = entity.getContentType();
      String result = "";
      if (entity != null) {
        result = EntityUtils.toString(entity);
      }

      return result;
    }

  }

  public void setKafkaClusterId(String kafkaClusterID) {
    this.kafkaClusterID = kafkaClusterID;
  }
}
