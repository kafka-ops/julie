package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SetAclsForConsumer extends BaseAccessControlAction {

  private final String fullTopicName;
  private final List<Consumer> consumers;
  private final AccessControlProvider controlProvider;

  public SetAclsForConsumer(
      AccessControlProvider controlProvider, List<Consumer> consumers, String fullTopicName) {
    super();
    this.consumers = consumers;
    this.fullTopicName = fullTopicName;
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() {
    bindings = controlProvider.setAclsForConsumers(consumers, fullTopicName);
  }

  @Override
  Map<String, Object> props() {
    List<String> principals =
        consumers.stream().map(c -> c.getPrincipal()).collect(Collectors.toList());
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", principals);
    map.put("Topic", fullTopicName);
    return map;
  }
}
