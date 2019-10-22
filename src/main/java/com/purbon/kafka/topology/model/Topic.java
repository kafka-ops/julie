package com.purbon.kafka.topology.model;

import java.util.HashMap;

public class Topic {

  private String name;
  private HashMap<String, String> config;

  public Topic(String name) {
    this(name, new HashMap<>());
  }

  public Topic(String name, HashMap<String, String> config) {
    this.name = name;
    this.config = config;
  }

  public Topic() {
    this("default", new HashMap<>());
  }


  public String getName() {
    return name;
  }

  public String composeTopicName(Topology topology, String projectName) {
    StringBuilder sb = new StringBuilder();
    sb.append(topology.getTeam())
        .append(".")
        .append(topology.getSource())
        .append(".")
        .append(projectName)
        .append(".")
        .append(getName());
    return sb.toString();
  }

  public void setName(String name) {
    this.name = name;
  }

  public HashMap<String, String> getConfig() {
    return config;
  }

  public void setConfig(HashMap<String, String> config) {
    this.config = config;
  }
}
