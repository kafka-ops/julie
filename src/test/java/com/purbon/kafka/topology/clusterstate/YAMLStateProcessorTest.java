package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class YAMLStateProcessorTest {

    @Test
    public void testWriteAndReadState() throws Exception {
        final List<TopologyAclBinding> bindings =
                Arrays.asList(
                        new TopologyAclBinding(ResourceType.TOPIC, "Resource1", "Host1", "Op1", "User1", "Pattern1"),
                        new TopologyAclBinding(ResourceType.TOPIC, "Resource2", "Host2", "Op2", "User2", "Pattern2"));
        final ClusterState clusterStateIn = new ClusterState(bindings);

        try (final StringWriter stringWriter = new StringWriter()) {
            final StateProcessor stateProcessor = new YAMLStateProcessor();

            // Writing
            stateProcessor.writeState(stringWriter, clusterStateIn);
            final String state = stringWriter.toString();

            try (final StringReader stringReader = new StringReader(state)) {
                // Reading
                final ClusterState clusterStateOut = stateProcessor.readState(stringReader);
                Assert.assertEquals(2, clusterStateOut.getAclBindings().size());
            }
        }
    }
}
