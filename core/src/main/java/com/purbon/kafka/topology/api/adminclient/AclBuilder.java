package com.purbon.kafka.topology.api.adminclient;

import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

public class AclBuilder {

  private ResourcePattern resourcePattern;
  private AccessControlEntry entry;
  private String principal;

  public AclBuilder(String principal) {
    this.principal = principal;
  }

  public AclBuilder addResource(ResourceType resourceType, String name, PatternType patternType) {
    resourcePattern = new ResourcePattern(resourceType, name, patternType);
    return this;
  }

  public AclBuilder addControlEntry(
      String host, AclOperation op, AclPermissionType permissionType) {
    entry = new AccessControlEntry(principal, host, op, permissionType);
    return this;
  }

  public AclBinding build() {
    return new AclBinding(resourcePattern, entry);
  }
}
