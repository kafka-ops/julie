package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Test;

public class YAMLStateProcessorTest {

  @Test
  public void testWriteAndReadState() throws Exception {
    final List<TopologyAclBinding> bindings =
        Arrays.asList(
            new TopologyAclBinding(ResourceType.TOPIC, "A1", "A2", "A3", "A4", "A5"),
            new TopologyAclBinding(ResourceType.TOPIC, "B1", "B2", "B3", "B4", "B5"));
    final ClusterState clusterStateIn = new ClusterState(bindings);

    try (final StringWriter stringWriter = new StringWriter()) {
      final StateProcessor stateProcessor = new YAMLStateProcessor();

      // Writing
      stateProcessor.writeState(stringWriter, clusterStateIn);
      final String state = stringWriter.toString();

      System.out.println(state);

      try (final StringReader stringReader = new StringReader(state)) {
        // Reading
        final ClusterState clusterStateOut = stateProcessor.readState(stringReader);
        Assert.assertEquals(2, clusterStateOut.getAclBindings().size());
      }
    }
  }
}
