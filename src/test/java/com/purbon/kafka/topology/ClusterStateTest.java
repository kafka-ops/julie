package com.purbon.kafka.topology;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.clusterstate.FileBackend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ClusterStateTest {

  @Mock FileBackend fileStateProcessor;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  public void testClusterStateRecovery() throws IOException {

    ClusterState backend = new ClusterState(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    when(fileStateProcessor.load()).thenReturn(Collections.singleton(binding));
    backend.load();
    verify(fileStateProcessor, times(1)).load();
  }

  @Test
  public void testClusterStateSize() {

    ClusterState backend = new ClusterState(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    backend.add(binding);

    assertEquals(1, backend.size());
  }
}
