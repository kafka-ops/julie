package com.purbon.kafka.topology.actions.access.builders.rbac;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildBindingsForSchemaAuthorization extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final Schemas schemaAuthorization;

  public BuildBindingsForSchemaAuthorization(
      BindingsBuilderProvider builderProvider, Schemas schemaAuthorization) {
    this.builderProvider = builderProvider;
    this.schemaAuthorization = schemaAuthorization;
  }

  @Override
  protected void execute() throws IOException {
    aclBindings =
        builderProvider.setSchemaAuthorization(
            schemaAuthorization.getPrincipal(),
            schemaAuthorization.getSubjects(),
            schemaAuthorization.getRole(),
            schemaAuthorization.isPrefixed());
  }

  @Override
  protected String resourceNameBuilder(TopologyAclBinding binding) {
    return String.format(
        "rn://create.binding.schema/%s/%s/%s/%s/%s",
        getClass().getName(),
        schemaAuthorization.getPrincipal(),
        schemaAuthorization.getSubjects().stream()
            .map(String::toLowerCase)
            .collect(Collectors.joining(",")),
        schemaAuthorization.getRole(),
        schemaAuthorization.isPrefixed());
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", schemaAuthorization.getPrincipal());
    map.put("Subjects", schemaAuthorization.getSubjects());
    map.put("Role", schemaAuthorization.getRole());
    map.put("IsPrefixed", schemaAuthorization.isPrefixed());
    return map;
  }
}
