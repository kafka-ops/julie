package com.purbon.kafka.topology;

import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccessControlWithRBACTest {

  @Mock RBACProvider aclsProvider;
  @Mock RBACBindingsBuilder bindingsBuilder;

  @Mock ExecutionPlan plan;
  @Mock BackendController backendController;

  private AccessControlManager accessControlManager;

  @BeforeEach
  public void setup() throws IOException {
    accessControlManager = new AccessControlManager(aclsProvider, bindingsBuilder);
    plan = ExecutionPlan.init(backendController, System.out);
  }

  @Test
  void predefinedRoles() throws IOException {
    Map<String, List<String>> predefinedRoles = new HashMap<>();
    predefinedRoles.put("ResourceOwner", Collections.singletonList("User:Foo"));

    Project project = new ProjectImpl();
    project.setRbacRawRoles(predefinedRoles);

    Topic topicA = new Topic("topicA");
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

    accessControlManager.updatePlan(topology, plan);

    verify(bindingsBuilder, times(1))
        .setPredefinedRole(eq("User:Foo"), eq("ResourceOwner"), eq(project.namePrefix()));
  }
}
