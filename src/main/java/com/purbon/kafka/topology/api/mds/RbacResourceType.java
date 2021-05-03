package com.purbon.kafka.topology.api.mds;

public class RbacResourceType {

  private String resourceType;
  private String name;
  private String patternType;

  public RbacResourceType() {
    this("", "", "");
  }

  public RbacResourceType(String resourceType, String name, String patternType) {
    this.resourceType = resourceType;
    this.name = name;
    this.patternType = patternType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getName() {
    return name;
  }

  public String getPatternType() {
    return patternType;
  }

  @Override
  public String toString() {
    return "RbacResourceType{"
        + "resourceType='"
        + resourceType
        + '\''
        + ", name='"
        + name
        + '\''
        + ", patternType='"
        + patternType
        + '\''
        + '}';
  }
}
