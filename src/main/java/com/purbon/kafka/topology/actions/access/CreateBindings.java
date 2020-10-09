package com.purbon.kafka.topology.actions.access;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateBindings extends BaseAccessControlAction {

  private static final Logger LOGGER = LogManager.getLogger(CreateBindings.class);

  private final AccessControlProvider controlProvider;

  public CreateBindings(AccessControlProvider controlProvider, Set<TopologyAclBinding> bindings) {
    super(bindings);
    this.controlProvider = controlProvider;
  }

  @Override
  protected void execute() throws IOException {
    LOGGER.debug("CreateBindings: " + bindings);
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
