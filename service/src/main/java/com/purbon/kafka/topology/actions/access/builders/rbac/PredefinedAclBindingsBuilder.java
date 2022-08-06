package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsBuilder;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsResult;
import java.util.Collections;

public class PredefinedAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final String principal;
  private final String predefinedRole;
  private final String topicPrefix;

  public PredefinedAclBindingsBuilder(
      BindingsBuilderProvider builderProvider,
      String principal,
      String predefinedRole,
      String topicPrefix) {
    this.builderProvider = builderProvider;
    this.principal = principal;
    this.predefinedRole = predefinedRole;
    this.topicPrefix = topicPrefix;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        Collections.singletonList(
            builderProvider.setPredefinedRole(principal, predefinedRole, topicPrefix)));
  }
}
