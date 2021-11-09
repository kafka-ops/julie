package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.actions.access.builders.AclBindingsBuilder;
import com.purbon.kafka.topology.actions.access.builders.AclBindingsResult;
import java.io.IOException;

public class SchemaAuthorizationAclBindingsBuilder implements AclBindingsBuilder {

  private final BuildBindingsForSchemaAuthorization schemaAuthorization;

  public SchemaAuthorizationAclBindingsBuilder(
      BuildBindingsForSchemaAuthorization schemaAuthorization) {
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    try {
      schemaAuthorization.run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return AclBindingsResult.forAclBindings(schemaAuthorization.getAclBindings());
  }
}
