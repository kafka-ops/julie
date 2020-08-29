package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SetAclsForControlCenter extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final ControlCenterInstance controlCenter;

  public SetAclsForControlCenter(
      AccessControlProvider controlProvider, ControlCenterInstance controlCenter) {
    super();
    this.controlProvider = controlProvider;
    this.controlCenter = controlCenter;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setAclsForControlCenter(
            controlCenter.getPrincipal(), controlCenter.getAppId());
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", controlCenter.getPrincipal());
    map.put("AppId", controlCenter.getAppId());

    try {
      return JSON.asString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

}
