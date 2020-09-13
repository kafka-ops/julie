package com.purbon.kafka.topology.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetAclsForProducer extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final List<Producer> producers;
  private final String fullTopicName;

  public SetAclsForProducer(
      AccessControlProvider controlProvider, List<Producer> producers, String fullTopicName) {
    super();
    this.controlProvider = controlProvider;
    this.producers = producers;
    this.fullTopicName = fullTopicName;
  }

  @Override
  public void run() throws IOException {
    Stream<String> producersStream = producers.stream().map(p -> p.getPrincipal());
    bindings =
        controlProvider.setAclsForProducers(
            producersStream.collect(Collectors.toList()), fullTopicName);
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put(
        "Principals", producers.stream().map(p -> p.getPrincipal()).collect(Collectors.toList()));
    map.put("Topic", fullTopicName);

    try {
      return JSON.asPrettyString(map);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
