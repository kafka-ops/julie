package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import java.io.IOException;

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
}
