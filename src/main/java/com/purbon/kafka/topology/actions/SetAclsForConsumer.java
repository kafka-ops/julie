package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.utils.JSON;
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
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put(
        "Principals", consumers.stream().map(c -> c.getPrincipal()).collect(Collectors.toList()));
    map.put("Topic", fullTopicName);

    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
