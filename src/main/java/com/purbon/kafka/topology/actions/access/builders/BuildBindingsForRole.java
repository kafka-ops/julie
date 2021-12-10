package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.JulieRole;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.utils.JinjaUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildBindingsForRole extends BaseAccessControlAction {

  private final BindingsBuilderProvider bindingsBuilder;
  private final JulieRole julieRole;
  private final List<Other> values;

  public BuildBindingsForRole(
      BindingsBuilderProvider bindingsBuilder, JulieRole julieRole, List<Other> values) {
    this.bindingsBuilder = bindingsBuilder;
    this.julieRole = julieRole;
    this.values = values;
  }

  @Override
  protected void execute() throws IOException {
    for (Other other : values) {
      var acls =
          julieRole.getAcls().stream()
              .map(
                  acl -> {
                    String resourceName =
                        JinjaUtils.serialise(acl.getResourceName(), other.asMap());
                    return new JulieRoleAcl(
                        acl.getResourceType(),
                        resourceName,
                        acl.getPatternType(),
                        acl.getHost(),
                        acl.getOperation(),
                        acl.getPermissionType());
                  })
              .collect(Collectors.toList());
      var instanceBindings =
          bindingsBuilder.buildBindingsForJulieRole(other, julieRole.getName(), acls);
      aclBindings.addAll(instanceBindings);
    }
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Role", julieRole.getName());
    return map;
  }
}
