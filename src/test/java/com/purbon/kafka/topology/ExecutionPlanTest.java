package com.purbon.kafka.topology;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.access.CreateBindings;
import com.purbon.kafka.topology.actions.topics.CreateTopicAction;
import com.purbon.kafka.topology.actions.topics.DeleteTopics;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ExecutionPlanTest {

  private ExecutionPlan plan;
  private BackendController backendController;

  @Mock PrintStream mockPrintStream;

  @Mock SimpleAclsProvider aclsProvider;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock TopologyBuilderAdminClient adminClient;

  @Mock SchemaRegistryManager schemaRegistryManager;

  @Before
  public void before() throws IOException {
    TestUtils.deleteStateFile();
    backendController = new BackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
  }

  @Test
  public void addBindingsTest() throws IOException {
    TopologyAclBinding binding1 =
        new TopologyAclBinding(
            ResourceType.ANY.name(), "topicA", "*", "ALL", "User:foo", "LITERAL");
    TopologyAclBinding binding2 =
        new TopologyAclBinding(
            ResourceType.ANY.name(), "topicB", "*", "ALL", "User:foo", "LITERAL");
    Set<TopologyAclBinding> bindings = new HashSet<>(Arrays.asList(binding1, binding2));
    CreateBindings addBindingsAction = new CreateBindings(aclsProvider, bindings);

    plan.add(addBindingsAction);

    plan.run();

    verify(aclsProvider, times(1)).createBindings(bindings);
    assertEquals(2, backendController.size());
  }

  @Test
  public void deleteBindingsAfterCreateTest() throws IOException {
    TopologyAclBinding binding1 =
        new TopologyAclBinding(
            ResourceType.ANY.name(), "topicA", "*", "ALL", "User:foo", "LITERAL");
    TopologyAclBinding binding2 =
        new TopologyAclBinding(
            ResourceType.ANY.name(), "topicB", "*", "ALL", "User:foo", "LITERAL");
    Set<TopologyAclBinding> bindings = new HashSet<>(Arrays.asList(binding1, binding2));
    CreateBindings addBindingsAction = new CreateBindings(aclsProvider, bindings);

    plan.add(addBindingsAction);

    plan.run();

    verify(aclsProvider, times(1)).createBindings(bindings);
    assertEquals(2, backendController.size());

    BackendController backendController = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(backendController, mockPrintStream);

    bindings = new HashSet<>(singletonList(binding2));
    ClearBindings clearBindingsAction = new ClearBindings(aclsProvider, bindings);

    plan.add(clearBindingsAction);

    plan.run();

    verify(aclsProvider, times(1)).clearBindings(bindings);
    assertEquals(1, backendController.size());

    backendController = new BackendController();
    backendController.load();
    assertEquals(1, backendController.size());
    backendController.flushAndClose();
  }

  @Test
  public void addTopicsTest() throws IOException {
    Topology topology = buildTopologyForTest();
    Topic topicFoo = topology.getProjects().get(0).getTopics().get(0);
    Topic topicBar = topology.getProjects().get(0).getTopics().get(1);
    Set<String> listOfTopics = new HashSet<>();

    CreateTopicAction addTopicAction1 =
        new CreateTopicAction(adminClient, topicFoo, topicFoo.toString());

    CreateTopicAction addTopicAction2 =
        new CreateTopicAction(adminClient, topicBar, topicBar.toString());

    plan.add(addTopicAction1);
    plan.add(addTopicAction2);

    plan.run();

    verify(adminClient, times(1)).createTopic(topicFoo, topicFoo.toString());
    verify(adminClient, times(1)).createTopic(topicBar, topicBar.toString());
    assertEquals(2, backendController.size());
  }

  @Test
  public void deleteTopicsPreviouslyAddedTest() throws IOException {
    Topology topology = buildTopologyForTest();
    Topic topicFoo = topology.getProjects().get(0).getTopics().get(0);
    Topic topicBar = topology.getProjects().get(0).getTopics().get(1);
    Set<String> listOfTopics = new HashSet<>();

    CreateTopicAction addTopicAction1 =
        new CreateTopicAction(adminClient, topicFoo, topicFoo.toString());

    CreateTopicAction addTopicAction2 =
        new CreateTopicAction(adminClient, topicBar, topicBar.toString());

    plan.add(addTopicAction1);
    plan.add(addTopicAction2);

    plan.run();

    verify(adminClient, times(1)).createTopic(topicFoo, topicFoo.toString());
    verify(adminClient, times(1)).createTopic(topicBar, topicBar.toString());
    assertEquals(2, backendController.size());

    BackendController backendController = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(backendController, mockPrintStream);

    DeleteTopics deleteTopicsAction =
        new DeleteTopics(adminClient, singletonList(topicFoo.toString()));
    plan.add(deleteTopicsAction);

    plan.run();

    verify(adminClient, times(1)).deleteTopics(singletonList(topicFoo.toString()));
    assertEquals(1, backendController.size());
  }

  private Topology buildTopologyForTest() {
    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("project");
    topology.setProjects(singletonList(project));

    Topic topic = new TopicImpl("foo");
    Topic topicBar = new TopicImpl("bar");
    project.setTopics(Arrays.asList(topic, topicBar));
    return topology;
  }
}
