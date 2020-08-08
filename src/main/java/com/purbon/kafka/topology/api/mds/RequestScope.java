package com.purbon.kafka.topology.api.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.utils.JSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    resource.put(RESOURCE_TYPE, resourceType);
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

  public String asJson() {
    try {
      return JSON.asString(scope);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return "";
    }
  }

  public Map<String, String> getClusterIDs() {
    return ((Map<String, Map<String, String>>) scope.get("scope")).get("clusters");
  }
}
