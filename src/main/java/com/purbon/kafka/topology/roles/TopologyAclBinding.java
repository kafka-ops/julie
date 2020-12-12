package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.api.mds.RequestScope;
import java.util.Objects;
import java.util.Optional;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class TopologyAclBinding implements Comparable<TopologyAclBinding> {

  private Optional<AclBinding> aclBindingOptional;

  private ResourceType resourceType;
  private String resourceName;
  private String host;
  private String operation;
  private String principal;
  private String pattern;

  /**
   * Topology ACL binding wrapper class constructor
   *
   * @param resourceType The resource type as described in ResourceType
   * @param resourceName The resource name
   * @param host the host this acl is allowed to
   * @param operation an operation
   * @param principal the selected principal
   * @param pattern a pattern to match this acl
   */
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
    this.aclBindingOptional = Optional.empty();
  }

  /**
   * Build method
   *
   * @param resourceTypeString
   * @param resourceName
   * @param host
   * @param operation
   * @param principal
   * @param pattern
   * @return
   */
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

  public TopologyAclBinding() {
    this(ResourceType.ANY, "", "", "", "", "");
  }

  public TopologyAclBinding(AclBinding binding) {

    this.aclBindingOptional = Optional.of(binding);

    AccessControlEntry entry = binding.entry();
    ResourcePattern pattern = binding.pattern();

    this.resourceType = pattern.resourceType();
    this.resourceName = pattern.name();
    this.principal = entry.principal();
    this.operation = entry.operation().name();
    this.pattern = pattern.patternType().name();
    this.host = entry.host();
  }

  public Optional<AclBinding> asAclBinding() {
    return aclBindingOptional;
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
    return "\'"
        + resourceType
        + '\''
        + ", '"
        + resourceName
        + '\''
        + ", '"
        + host
        + '\''
        + ", '"
        + operation
        + '\''
        + ", '"
        + principal
        + '\''
        + ", '"
        + pattern
        + '\'';
  }

  public String getResourceName() {
    return resourceName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopologyAclBinding binding = (TopologyAclBinding) o;
    return getResourceType() == binding.getResourceType()
        && getResourceName().equals(binding.getResourceName())
        && getHost().equals(binding.getHost())
        && getOperation().equals(binding.getOperation())
        && getPrincipal().equals(binding.getPrincipal())
        && getPattern().equals(binding.getPattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getResourceType(),
        getResourceName(),
        getHost(),
        getOperation(),
        getPrincipal(),
        getPattern());
  }

  public String getRole() {
    return operation;
  }

  private RequestScope scope;

  public void setScope(RequestScope scope) {
    this.scope = scope;
  }

  public RequestScope getScope() {
    return scope;
  }

  @Override
  public int compareTo(TopologyAclBinding o) {
    return toString().compareTo(o.toString());
  }
}
