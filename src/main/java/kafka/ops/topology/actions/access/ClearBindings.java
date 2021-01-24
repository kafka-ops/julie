package kafka.ops.topology.actions.access;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import kafka.ops.topology.AccessControlProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.roles.TopologyAclBinding;
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
