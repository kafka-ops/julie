package com.purbon.kafka.topology.api.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.purbon.kafka.topology.serdes.RequestScopeDeser;
import com.purbon.kafka.topology.serdes.RequestScopeSerde;
import com.purbon.kafka.topology.utils.JSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@JsonDeserialize(using = RequestScopeDeser.class)
@JsonSerialize(using = RequestScopeSerde.class)
public class RequestScope {

  public static final String RESOURCE_TYPE = "resourceType";
  public static final String RESOURCE_NAME = "name";
  public static final String RESOURCE_PATTERN_TYPE = "patternType";

  private Map<String, Object> scope;
  private Map<String, Map<String, String>> clusters;
  private List<Map<String, String>> resources;

  public RequestScope() {
    this.scope = new HashMap<>();
    this.clusters = new HashMap<>();
    this.resources = new ArrayList<>();
  }

  public void setClusters(Map<String, Map<String, String>> clusters) {
    this.clusters = clusters;
  }

  public void addResource(String resourceType, String name, String patternType) {
    Map<String, String> resource = new HashMap<>();
    resource.put(RESOURCE_TYPE, StringUtils.capitalize(resourceType.toLowerCase()));
    resource.put(RESOURCE_NAME, name);
    resource.put(RESOURCE_PATTERN_TYPE, patternType);
    this.resources.add(resource);
  }

  public Map<String, String> getResource(int index) {
    return this.resources.get(index);
  }

  public void build() {
    scope = new HashMap<>();
    scope.put("scope", clusters);
    scope.put("resourcePatterns", resources);
  }

  public String clustersAsJson() {
    try {
      return JSON.asString(clusters);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return "";
    }
  }

  public String asJson() {
    try {
      return JSON.asString(scope);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return "";
    }
  }

  public Map<String, Map<String, String>> getScope() {
    return clusters;
  }

  public List<Map<String, String>> getResources() {
    return resources;
  }

  public Map<String, String> clusterIDs() {
    return ((Map<String, Map<String, String>>) scope.get("scope")).get("clusters");
  }
}
