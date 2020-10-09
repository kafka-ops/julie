package com.purbon.kafka.topology.actions.access;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClearBindings extends BaseAccessControlAction {

  private static final Logger LOGGER = LogManager.getLogger(ClearBindings.class);

  private final AccessControlProvider controlProvider;

  public ClearBindings(
      AccessControlProvider controlProvider, Collection<TopologyAclBinding> bindingsForRemoval) {
    super(bindingsForRemoval);
    this.controlProvider = controlProvider;
  }

  @Override
  protected void execute() throws IOException {
    LOGGER.debug("ClearBindings: " + bindings);
    controlProvider.clearBindings(new HashSet(bindings));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Bindings", bindings);
    return map;
  }
}
