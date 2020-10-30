package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildBindingsForProducer extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final List<Producer> producers;
  private final String fullTopicName;

  public BuildBindingsForProducer(
      BindingsBuilderProvider builderProvider, List<Producer> producers, String fullTopicName) {
    super();
    this.builderProvider = builderProvider;
    this.producers = producers;
    this.fullTopicName = fullTopicName;
  }

  @Override
  protected void execute() throws IOException {
    bindings = builderProvider.buildBindingsForProducers(producers, fullTopicName);
  }

  @Override
  protected Map<String, Object> props() {
    List<String> principals =
        producers.stream().map(p -> p.getPrincipal()).collect(Collectors.toList());
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", principals);
    map.put("Topic", fullTopicName);
    return map;
  }
}
