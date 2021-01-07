package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class ServiceAccountDeleteRequest {

  @JsonProperty("id")
  private int id;

  public ServiceAccountDeleteRequest() {
  }

  public ServiceAccountDeleteRequest(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "ServiceAccountDeleteRequest{" +
            "id=" + id +
            '}';
  }
}