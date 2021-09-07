package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;
import java.util.HashMap;
import java.util.Map;

public class BuildBindingsForKSqlServer extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final KsqlServerInstance ksqlServer;

  public BuildBindingsForKSqlServer(
      BindingsBuilderProvider builderProvider, KsqlServerInstance ksqlServer) {
    super();
    this.builderProvider = builderProvider;
    this.ksqlServer = ksqlServer;
  }

  @Override
  protected void execute() {
    bindings = builderProvider.buildBindingsForKSqlServer(ksqlServer);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", ksqlServer.getPrincipal());
    map.put("KsqlDbId", ksqlServer.getKsqlDbId());
    return map;
  }
}
