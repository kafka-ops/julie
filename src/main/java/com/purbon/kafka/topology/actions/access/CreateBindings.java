package com.purbon.kafka.topology.actions.access;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateBindings extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;

  public CreateBindings(AccessControlProvider controlProvider, Set<TopologyAclBinding> bindings) {
    super(bindings);
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() throws IOException {
    controlProvider.createBindings(new HashSet<>(bindings));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Bindings", bindings);
    return map;
  }
}
