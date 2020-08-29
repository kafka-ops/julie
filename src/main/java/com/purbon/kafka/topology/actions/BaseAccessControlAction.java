package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseAccessControlAction implements Action {

  protected List<TopologyAclBinding> bindings;

  public BaseAccessControlAction() {
    this.bindings = new ArrayList<>();
  }

  @Override
  public List<TopologyAclBinding> getBindings() {
    return bindings;
  }
}
