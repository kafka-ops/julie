package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Schemas;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForSchemaAuthorization extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final Schemas schemaAuthorization;

  public BuildBindingsForSchemaAuthorization(
      BindingsBuilderProvider builderProvider, Schemas schemaAuthorization) {
    super();
    this.builderProvider = builderProvider;
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  protected void execute() throws IOException {
    bindings =
        builderProvider.setSchemaAuthorization(
            schemaAuthorization.getPrincipal(), schemaAuthorization.getSubjects());
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", schemaAuthorization.getPrincipal());
    map.put("Subjects", schemaAuthorization.getSubjects());
    return map;
  }
}
