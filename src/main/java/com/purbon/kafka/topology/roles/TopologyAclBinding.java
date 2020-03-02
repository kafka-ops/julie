package com.purbon.kafka.topology.roles;

import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class TopologyAclBinding {

  private ResourceType resourceType;
  private String cluster;
  private String host;
  private String operation;
  private String principal;
  private String topic;
  private String group;
  private String pattern;

  public TopologyAclBinding(AclBinding binding) {

    AccessControlEntry entry = binding.entry();
    ResourcePattern pattern = binding.pattern();

    this.resourceType = pattern.resourceType();

    this.principal = entry.principal();
    this.operation = entry.operation().name();

    if (resourceType.equals(ResourceType.TOPIC)) {
      this.topic = pattern.name();
    } else if (resourceType.equals(ResourceType.GROUP)) {
      this.group = pattern.name();
    } else if (resourceType.equals(ResourceType.CLUSTER)) {
      this.cluster = pattern.name();
    }

    this.pattern = pattern.patternType().name();
    this.host = entry.host();

  }

  public String getResourceType() {
    return resourceType.name();
  }

  public String getPattern() {
    return pattern;
  }

  public String getGroup() {
    return group;
  }

  public String getTopic() {
    return topic;
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

  public String getCluster() {
    return cluster;
  }

  @Override
  public String toString() {
    return  "\'" + resourceType + '\'' +
        ", '" + cluster + '\'' +
        ", '" + host + '\'' +
        ", '" + operation + '\'' +
        ", '" + principal + '\'' +
        ", '" + topic + '\'' +
        ", '" + group + '\'' +
        ", '" + pattern + '\'' ;
  }
}
