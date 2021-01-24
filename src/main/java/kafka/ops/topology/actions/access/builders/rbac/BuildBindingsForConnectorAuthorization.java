package kafka.ops.topology.actions.access.builders.rbac;

import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.model.users.Connector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForConnectorAuthorization extends BaseAccessControlAction {

  private final Connector connector;
  private final BindingsBuilderProvider builderProvider;

  public BuildBindingsForConnectorAuthorization(
      BindingsBuilderProvider builderProvider, Connector connector) {
    super();
    this.builderProvider = builderProvider;
    this.connector = connector;
  }

  @Override
  protected void execute() {
    bindings =
        builderProvider.setConnectorAuthorization(
            connector.getPrincipal(), connector.getConnectors().orElse(new ArrayList<>()));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("clusterId", connector.getCluster_id());
    return map;
  }
}
