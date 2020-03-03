package com.purbon.kafka.topology.roles;

import java.io.IOException;
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

  public TopologyAclBinding(
      ResourceType resourceType,
      String cluster,
      String host,
      String operation,
      String principal,
      String topic,
      String group,
      String pattern
  ) {
    this.resourceType = resourceType;
    this.cluster = cluster;
    this.host = host;
    this.operation =  operation;
    this.principal = principal;
    this.topic = topic;
    this.group = group;
    this.pattern = pattern;
  }

  public static TopologyAclBinding build(String resourceTypeString,
      String cluster,
      String host,
      String operation,
      String principal,
      String topic,
      String group,
      String pattern) {

    ResourceType resourceType = ResourceType.valueOf(resourceTypeString);
    return new TopologyAclBinding(resourceType,
        cluster,
        host,
        operation,
        principal,
        topic,
        group,
        pattern);
  }

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

  public ResourceType getResourceType() {
    return resourceType;
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

  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
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

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
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

  public String getResourceName() {
    if (resourceType.equals(ResourceType.TOPIC)) {
      return topic;
    } else if (resourceType.equals(ResourceType.GROUP)) {
      return group;
    } else if (resourceType.equals(ResourceType.CLUSTER)) {
      return cluster;
    } else {
      return "";
    }
  }
}
