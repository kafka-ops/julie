package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Connector;
import java.util.HashMap;
import java.util.Map;

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
  protected void execute() {
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
