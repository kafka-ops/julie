package com.purbon.kafka.topology.model;

import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.serdes.FlatDescriptionSerde;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Test;

public class FlatDescriptionTest {

  @Test
  public void jsonSerializationDeserialization() {
    Set<TopologyAclBinding> aclBindings = new HashSet<>();
    aclBindings.add(
        new TopologyAclBinding(
            ResourceType.TOPIC, "test-topic", "*", "WRITE", "User:alice", "LITERAL"));
    aclBindings.add(
        new TopologyAclBinding(
            ResourceType.TOPIC, "test-topic", "*", "READ", "User:bob", "LITERAL"));

    Map<String, TopicDescription> topics = new HashMap<>();
    topics.put(
        "test-topic", new TopicDescription("test-topic", Collections.singletonMap("hhh", "vvvv")));

    final FlatDescription originalFlatDescription = new FlatDescription(aclBindings, topics);

    final String serialized = FlatDescriptionSerde.convertToJsonString(originalFlatDescription);
    final FlatDescription afterRoundtrip = FlatDescriptionSerde.convertFromJsonString(serialized);

    assertEquals(originalFlatDescription, afterRoundtrip);
  }

}
