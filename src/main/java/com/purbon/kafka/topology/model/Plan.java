package com.purbon.kafka.topology.model;

import java.util.HashMap;
import java.util.Map;

public class Plan {

  private String name;
  private Map<String, Object> config;

  public Plan() {
    this("default", new HashMap<>());
  }

  public Plan(String name, Map<String, Object> config) {
    this.name = name;
    this.config = config;
  }

  public String getName() {
    return name;
  }

  public Map<String, Object> getConfig() {
    return config;
  }
}
