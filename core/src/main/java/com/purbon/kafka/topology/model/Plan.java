package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.Map;

public class Plan {

  @JsonInclude(Include.NON_EMPTY)
  private String alias;

  private Map<String, String> config;

  public Plan() {
    this("default", new HashMap<>());
  }

  public Plan(String alias, Map<String, String> config) {
    this.alias = alias;
    this.config = config;
  }

  public String getAlias() {
    return alias;
  }

  public Map<String, String> getConfig() {
    return config;
  }
}
