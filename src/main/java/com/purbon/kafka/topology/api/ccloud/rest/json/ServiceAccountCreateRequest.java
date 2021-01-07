package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class ServiceAccountCreateRequest {

  @JsonProperty("service_name")
  private String name;

  @JsonProperty("service_description")
  private String description;

  public ServiceAccountCreateRequest() {
  }

  public ServiceAccountCreateRequest(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}