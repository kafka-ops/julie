package kafka.ops.topology.actions.access;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import kafka.ops.topology.AccessControlProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.roles.TopologyAclBinding;
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
