package com.purbon.kafka.topology.roles;

import static org.junit.Assert.assertEquals;

import org.apache.kafka.common.resource.ResourceType;
import org.junit.Test;

public class TopologyAclBindingTest {
  @Test
  public void fromKafkaAclBinding() {
    TopologyAclBinding binding =
        new TopologyAclBinding(
            ResourceType.TOPIC, "test-topic", "hostname", "WRITE", "User:alice", "LITERAL");
    TopologyAclBinding converted = new TopologyAclBinding(binding.toAclBinding());

    assertEquals(binding, converted);
  }
}
