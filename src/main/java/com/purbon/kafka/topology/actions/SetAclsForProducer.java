package com.purbon.kafka.topology.actions;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SetAclsForProducer extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final Producer producer;
  private final String fullTopicName;

  public SetAclsForProducer(
      AccessControlProvider controlProvider, Producer producer, String fullTopicName) {
    super();
    this.controlProvider = controlProvider;
    this.producer = producer;
    this.fullTopicName = fullTopicName;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setAclsForProducers(singletonList(producer.getPrincipal()), fullTopicName);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", producer.getPrincipal());
    map.put("Topic", fullTopicName);

    try {
      return JSON.asString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
