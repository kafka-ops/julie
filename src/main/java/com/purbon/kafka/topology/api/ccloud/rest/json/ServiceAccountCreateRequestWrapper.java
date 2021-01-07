package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class ServiceAccountCreateRequestWrapper {

  @JsonProperty("user")
  private ServiceAccountCreateRequest serviceAccount;

  public ServiceAccountCreateRequestWrapper() {
  }

  public ServiceAccountCreateRequestWrapper(ServiceAccountCreateRequest serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public ServiceAccountCreateRequest getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ServiceAccountCreateRequest serviceAccount) {
    this.serviceAccount = serviceAccount;
  }
}