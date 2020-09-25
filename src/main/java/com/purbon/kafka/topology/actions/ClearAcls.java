package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ClearAcls extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;

  public ClearAcls(
      AccessControlProvider controlProvider, Collection<TopologyAclBinding> bindingsForRemoval) {
    super(bindingsForRemoval);
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() throws IOException {
    controlProvider.clearAcls(new HashSet(bindings));
  }

  @Override
  Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    return map;
  }
}
