package com.purbon.kafka.topology;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BackendControllerTest {

  @Mock FileBackend fileStateProcessor;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  public void testClusterStateRecovery() throws IOException {

    BackendController backend = new BackendController(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    when(fileStateProcessor.loadBindings()).thenReturn(Collections.singleton(binding));
    backend.load();
    verify(fileStateProcessor, times(1)).loadBindings();
  }

  @Test
  public void testClusterStateSize() {

    BackendController backend = new BackendController(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    backend.add(binding);

    assertEquals(1, backend.size());
  }

  @Test
  public void testStoreBindingsAndServiceAccounts() {

    BackendController backend = new BackendController(fileStateProcessor);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    ServiceAccount serviceAccount = new ServiceAccount(1, "name", "description");

    backend.add(Collections.singletonList(binding));
    backend.addServiceAccounts(Collections.singleton(serviceAccount));

    backend.flushAndClose();

    verify(fileStateProcessor, times(1)).saveBindings(Collections.singleton(binding));
    verify(fileStateProcessor, times(1)).saveAccounts(Collections.singleton(serviceAccount));
  }
}
