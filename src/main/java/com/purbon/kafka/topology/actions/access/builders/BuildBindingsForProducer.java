package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Producer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildBindingsForProducer extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final List<Producer> producers;
  private final String fullTopicName;
  private final boolean prefixed;

  public BuildBindingsForProducer(
      BindingsBuilderProvider builderProvider,
      List<Producer> producers,
      String fullTopicName,
      boolean prefixed) {
    super();
    this.builderProvider = builderProvider;
    this.producers = producers;
    this.fullTopicName = fullTopicName;
    this.prefixed = prefixed;
  }

  @Override
  protected void execute() {
    bindings = builderProvider.buildBindingsForProducers(producers, fullTopicName, prefixed);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", producers);
    map.put("Topic", fullTopicName);
    return map;
  }
}
