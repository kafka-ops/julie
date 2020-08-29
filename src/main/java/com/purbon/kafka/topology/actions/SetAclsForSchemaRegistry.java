package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import java.io.IOException;

public class SetAclsForSchemaRegistry extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final SchemaRegistryInstance schemaRegistry;

  public SetAclsForSchemaRegistry(
      AccessControlProvider controlProvider, SchemaRegistryInstance schemaRegistry) {
    super();
    this.controlProvider = controlProvider;
    this.schemaRegistry = schemaRegistry;
  }

  @Override
  public void run() throws IOException {
    bindings = controlProvider.setAclsForSchemaRegistry(schemaRegistry);
  }
}
