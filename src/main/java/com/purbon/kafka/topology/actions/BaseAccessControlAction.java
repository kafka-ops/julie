package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseAccessControlAction extends BaseAction {

  protected Collection<TopologyAclBinding> bindings;

  public BaseAccessControlAction() {
    this(new ArrayList<>());
  }

  public BaseAccessControlAction(Collection<TopologyAclBinding> bindings) {
    this.bindings = bindings;
  }

  @Override
  public List<TopologyAclBinding> getBindings() {
    return new ArrayList<>(bindings);
  }
}
