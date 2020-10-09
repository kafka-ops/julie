package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForControlCenter extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final ControlCenterInstance controlCenter;

  public BuildBindingsForControlCenter(
      BindingsBuilderProvider builderProvider, ControlCenterInstance controlCenter) {
    super();
    this.builderProvider = builderProvider;
    this.controlCenter = controlCenter;
  }

  @Override
  protected void execute() throws IOException {
    bindings =
        builderProvider.buildBindingsForControlCenter(
            controlCenter.getPrincipal(), controlCenter.getAppId());
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", controlCenter.getPrincipal());
    map.put("AppId", controlCenter.getAppId());
    return map;
  }
}
