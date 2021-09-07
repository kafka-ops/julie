package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsBuilder;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsResult;
import com.purbon.kafka.topology.model.users.Schemas;

public class SchemaAuthorizationAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final Schemas schemaAuthorization;

  public SchemaAuthorizationAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, Schemas schemaAuthorization) {
    this.builderProvider = builderProvider;
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        builderProvider.setSchemaAuthorization(
            schemaAuthorization.getPrincipal(), schemaAuthorization.getSubjects()));
  }
}
