package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Schemas;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForSchemaAuthorization extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final Schemas schemaAuthorization;

  public BuildBindingsForSchemaAuthorization(
      AccessControlProvider controlProvider, Schemas schemaAuthorization) {
    super();
    this.controlProvider = controlProvider;
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setSchemaAuthorization(
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
