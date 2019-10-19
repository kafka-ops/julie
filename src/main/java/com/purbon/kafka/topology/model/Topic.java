package com.purbon.kafka.topology.model;

import java.util.HashMap;

public class Topic {

  private String name;
  private HashMap<String, String> config;

  public Topic() {

  }


  public String getName() {
    return name;
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
