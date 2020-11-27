package com.purbon.kafka.topology;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.actions.access.CreateBindings;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
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

  ExecutionPlan plan;
  BackendController backendController;

  @Mock PrintStream mockPrintStream;

  @Mock SimpleAclsProvider aclsProvider;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void before() throws IOException {
    TestUtils.deleteStateFile();
    backendController = new BackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
  }

  @Test
  public void addBindingsTest() throws IOException {
    TopologyAclBinding binding1 =
        new TopologyAclBinding(ResourceType.ANY, "topicA", "*", "ALL", "User:foo", "LITERAL");
    TopologyAclBinding binding2 =
        new TopologyAclBinding(ResourceType.ANY, "topicB", "*", "ALL", "User:foo", "LITERAL");
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
        new TopologyAclBinding(ResourceType.ANY, "topicA", "*", "ALL", "User:foo", "LITERAL");
    TopologyAclBinding binding2 =
        new TopologyAclBinding(ResourceType.ANY, "topicB", "*", "ALL", "User:foo", "LITERAL");
    Set<TopologyAclBinding> bindings = new HashSet<>(Arrays.asList(binding1, binding2));
    CreateBindings addBindingsAction = new CreateBindings(aclsProvider, bindings);

    plan.add(addBindingsAction);

    plan.run();

    verify(aclsProvider, times(1)).createBindings(bindings);
    assertEquals(2, backendController.size());

    BackendController backendController = new BackendController();
    ExecutionPlan plan = ExecutionPlan.init(backendController, mockPrintStream);

    bindings = new HashSet<>(Arrays.asList(binding2));
    ClearBindings clearBindingsAction = new ClearBindings(aclsProvider, bindings);

    plan.add(clearBindingsAction);

    plan.run();

    verify(aclsProvider, times(1)).clearBindings(bindings);
    assertEquals(1, backendController.size());

    backendController = new BackendController();
    backendController.load();
    assertEquals(1, backendController.size());
  }
}
