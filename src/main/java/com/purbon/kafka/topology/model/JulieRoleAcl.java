package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JulieRoleAcl {

  private String resourceType;
  private String resourceName;
  private String patternType;
  private String host;
  private String operation;
  private String permissionType;

  @JsonCreator
  public JulieRoleAcl(
      @JsonProperty("resourceType") String resourceType,
      @JsonProperty("resourceName") String resourceName,
      @JsonProperty("patternType") String patternType,
      @JsonProperty("host") String host,
      @JsonProperty("operation") @JsonAlias("role") String operation,
      @JsonProperty("permissionType") String permissionType) {
    this.resourceType = resourceType;
    this.resourceName = resourceName;
    this.patternType = patternType;
    this.host = host;
    this.operation = operation;
    this.permissionType = permissionType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getPatternType() {
    return patternType;
  }

  public String getHost() {
    return host;
  }

  public String getOperation() {
    return operation;
  }

  public String getPermissionType() {
    return permissionType;
  }

  public String getRole() {
    return getOperation();
  }
}
