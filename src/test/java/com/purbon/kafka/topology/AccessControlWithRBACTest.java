package com.purbon.kafka.topology;

import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder;
import java.io.IOException;
import java.util.*;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlWithRBACTest {

  @Mock RBACProvider aclsProvider;
  @Mock RBACBindingsBuilder bindingsBuilder;

  @Mock ExecutionPlan plan;
  @Mock BackendController backendController;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AccessControlManager accessControlManager;

  @Before
  public void setup() throws IOException {
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder);
    plan = ExecutionPlan.init(backendController, System.out);
  }

  @Test
  public void testPredefinedRoles() throws IOException {
    Map<String, List<String>> predefinedRoles = new HashMap<>();
    predefinedRoles.put("ResourceOwner", Collections.singletonList("User:Foo"));

    Project project = new ProjectImpl();
    project.setRbacRawRoles(predefinedRoles);

    Topic topicA = new TopicImpl("topicA");
    project.addTopic(topicA);

    Topology topology = new TopologyImpl();
    topology.addProject(project);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(),
            "foo",
            "host",
            AclOperation.DESCRIBE_CONFIGS.name(),
            "User:Foo",
            PatternType.ANY.name());
    when(bindingsBuilder.setPredefinedRole("User:Foo", "ResourceOwner", project.namePrefix()))
        .thenReturn(binding);

    accessControlManager.updatePlan(plan, topology);

    verify(bindingsBuilder, times(1))
        .setPredefinedRole(eq("User:Foo"), eq("ResourceOwner"), eq(project.namePrefix()));
  }
}
