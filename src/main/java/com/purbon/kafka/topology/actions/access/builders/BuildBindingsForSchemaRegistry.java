package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForSchemaRegistry extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final SchemaRegistryInstance schemaRegistry;

  public BuildBindingsForSchemaRegistry(
      BindingsBuilderProvider builderProvider, SchemaRegistryInstance schemaRegistry) {
    super();
    this.builderProvider = builderProvider;
    this.schemaRegistry = schemaRegistry;
  }

  @Override
  protected void execute() throws IOException {
    bindings = builderProvider.buildBindingsForSchemaRegistry(schemaRegistry);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", schemaRegistry.getPrincipal());
    return map;
  }
}
