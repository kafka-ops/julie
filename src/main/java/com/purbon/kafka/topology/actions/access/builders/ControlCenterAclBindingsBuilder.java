package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;

public class ControlCenterAclBindingsBuilder implements AclBindingsBuilder {

  private final BindingsBuilderProvider builderProvider;
  private final ControlCenterInstance controlCenter;

  public ControlCenterAclBindingsBuilder(
      BindingsBuilderProvider builderProvider, ControlCenterInstance controlCenter) {
    this.builderProvider = builderProvider;
    this.controlCenter = controlCenter;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        builderProvider.buildBindingsForControlCenter(
            controlCenter.getPrincipal(), controlCenter.getAppId()));
  }
}
