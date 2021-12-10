package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;

public class SchemaRegistryAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final SchemaRegistryInstance schemaRegistry;

  public SchemaRegistryAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, SchemaRegistryInstance schemaRegistry) {
    this.builderProvider = builderProvider;
    this.schemaRegistry = schemaRegistry;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        builderProvider.buildBindingsForSchemaRegistry(schemaRegistry));
  }
}
