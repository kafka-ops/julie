package kafka.ops.topology.actions.access.builders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.model.users.Connector;

public class BuildBindingsForKConnect extends BaseAccessControlAction {

  private final Connector app;
  private final String topicPrefix;
  private final BindingsBuilderProvider controlProvider;

  public BuildBindingsForKConnect(
      BindingsBuilderProvider controlProvider, Connector app, String topicPrefix) {
    super();
    this.app = app;
    this.topicPrefix = topicPrefix;
    this.controlProvider = controlProvider;
  }

  @Override
  protected void execute() throws IOException {
    bindings = controlProvider.buildBindingsForConnect(app, topicPrefix);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", app.getPrincipal());
    map.put("Topic", topicPrefix);
    return map;
  }
}
