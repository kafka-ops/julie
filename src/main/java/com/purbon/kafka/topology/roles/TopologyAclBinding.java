package com.purbon.kafka.topology.roles;

import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class TopologyAclBinding {

  private ResourceType resourceType;
  private String resourceName;
  private String host;
  private String operation;
  private String principal;
  private String pattern;

  public TopologyAclBinding() {
  }

  public TopologyAclBinding(
      ResourceType resourceType,
      String resourceName,
      String host,
      String operation,
      String principal,
      String pattern) {
    this.resourceType = resourceType;
    this.resourceName = resourceName;
    this.host = host;
    this.operation = operation;
    this.principal = principal;
    this.pattern = pattern;
  }

  public static TopologyAclBinding build(
      String resourceTypeString,
      String resourceName,
      String host,
      String operation,
      String principal,
      String pattern) {

    ResourceType resourceType = ResourceType.valueOf(resourceTypeString);
    return new TopologyAclBinding(resourceType, resourceName, host, operation, principal, pattern);
  }

  public TopologyAclBinding(AclBinding binding) {

    AccessControlEntry entry = binding.entry();
    ResourcePattern pattern = binding.pattern();

    this.resourceType = pattern.resourceType();
    this.resourceName = pattern.name();
    this.principal = entry.principal();
    this.operation = entry.operation().name();
    this.pattern = pattern.patternType().name();
    this.host = entry.host();
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public String getPattern() {
    return pattern;
  }

  public String getPrincipal() {
    return principal;
  }

  public String getOperation() {
    return operation;
  }

  public String getHost() {
    return host;
  }

  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public String toString() {
    return "'" + resourceType + '\'' +
        ", '" + resourceName + '\'' +
        ", '" + host + '\'' +
        ", '" + operation + '\'' +
        ", '" + principal + '\'' +
        ", '" + pattern + '\'' ;
  }

  public String getResourceName() {
    return resourceName;
  }
}
