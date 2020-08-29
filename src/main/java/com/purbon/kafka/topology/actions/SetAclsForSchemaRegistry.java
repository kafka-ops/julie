package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", schemaRegistry.getPrincipal());

    try {
      return JSON.asString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
