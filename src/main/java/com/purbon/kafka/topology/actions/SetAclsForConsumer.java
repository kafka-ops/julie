package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.utils.JSON;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SetAclsForConsumer extends BaseAccessControlAction {

  private final String fullTopicName;
  private final Consumer consumer;
  private final AccessControlProvider controlProvider;

  public SetAclsForConsumer(
      AccessControlProvider controlProvider, Consumer consumer, String fullTopicName) {
    super();
    this.consumer = consumer;
    this.fullTopicName = fullTopicName;
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() {
    bindings =
        controlProvider.setAclsForConsumers(Collections.singletonList(consumer), fullTopicName);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", consumer.getPrincipal());
    map.put("Topic", fullTopicName);

    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
