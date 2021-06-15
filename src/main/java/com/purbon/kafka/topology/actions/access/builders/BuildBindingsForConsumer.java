package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildBindingsForConsumer extends BaseAccessControlAction {

  private final String fullTopicName;
  private final List<Consumer> consumers;
  private final BindingsBuilderProvider builderProvider;
  private boolean prefixed;

  public BuildBindingsForConsumer(
      BindingsBuilderProvider builderProvider,
      List<Consumer> consumers,
      String fullTopicName,
      boolean prefixed) {
    super();
    this.consumers = consumers;
    this.fullTopicName = fullTopicName;
    this.builderProvider = builderProvider;
    this.prefixed = prefixed;
  }

  @Override
  protected void execute() {
    aclBindings = builderProvider.buildBindingsForConsumers(consumers, fullTopicName, prefixed);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", consumers);
    map.put("Topic", fullTopicName);
    return map;
  }
}
