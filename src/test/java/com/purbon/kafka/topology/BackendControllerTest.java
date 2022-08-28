package com.purbon.kafka.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BackendControllerTest {

  @Mock FileBackend fileStateProcessor;

  @Test
  void testClusterStateRecovery() throws IOException {

    BackendController backend = new BackendController(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    when(fileStateProcessor.load()).thenReturn(new BackendState());
    backend.load();
    verify(fileStateProcessor, times(1)).load();
  }

  @Test
  void testClusterStateSize() {

    BackendController backend = new BackendController(fileStateProcessor);
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    backend.addBindings(Collections.singletonList(binding));

    assertEquals(1, backend.size());
  }

  @Test
  void testStoreBindingsAndServiceAccounts() throws IOException {

    BackendController backend = new BackendController(fileStateProcessor);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    ServiceAccount serviceAccount = new ServiceAccount("1", "name", "description");

    backend.addBindings(Collections.singletonList(binding));
    backend.addServiceAccounts(Collections.singleton(serviceAccount));

    backend.flushAndClose();

    verify(fileStateProcessor, times(1)).save(any(BackendState.class));
  }

  @Test
  void testStoreBindingsAndTopics() throws IOException {
    BackendController backend = new BackendController(fileStateProcessor);

    Topic topic = new Topic("foo");
    Project project = new ProjectImpl("project");
    project.addTopic(topic);
    Topology topology = new TopologyImpl();
    topology.setContext("context");
    topology.addProject(project);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    backend.addBindings(Collections.singletonList(binding));
    backend.addTopics(Collections.singleton(topic.getName()));

    backend.flushAndClose();
    verify(fileStateProcessor, times(1)).save(any(BackendState.class));
  }
}
