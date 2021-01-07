package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class ServiceAccountDeleteRequestWrapper {

  @JsonProperty("user")
  private ServiceAccountDeleteRequest serviceAccount;

  public ServiceAccountDeleteRequestWrapper() {
  }

  public ServiceAccountDeleteRequestWrapper(ServiceAccountDeleteRequest serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public ServiceAccountDeleteRequest getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ServiceAccountDeleteRequest serviceAccount) {
    this.serviceAccount = serviceAccount;
  }
}