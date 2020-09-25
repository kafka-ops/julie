package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForControlCenter extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final ControlCenterInstance controlCenter;

  public BuildBindingsForControlCenter(
      AccessControlProvider controlProvider, ControlCenterInstance controlCenter) {
    super();
    this.controlProvider = controlProvider;
    this.controlCenter = controlCenter;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.buildBindingsForControlCenter(
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
