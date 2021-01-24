package kafka.ops.topology;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kafka.ops.topology.model.Impl.ProjectImpl;
import kafka.ops.topology.model.Impl.TopicImpl;
import kafka.ops.topology.model.Impl.TopologyImpl;
import kafka.ops.topology.model.Project;
import kafka.ops.topology.model.Topic;
import kafka.ops.topology.model.Topology;
import kafka.ops.topology.roles.RbacProvider;
import kafka.ops.topology.roles.TopologyAclBinding;
import kafka.ops.topology.roles.rbac.RBACBindingsBuilder;
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

  @Mock RbacProvider aclsProvider;
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
  public void testPredefinedRoles() {
    Map<String, List<String>> predefinedRoles = new HashMap<>();
    predefinedRoles.put("ResourceOwner", Arrays.asList("User:Foo"));

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

    accessControlManager.apply(topology, plan);

    verify(bindingsBuilder, times(1))
        .setPredefinedRole(eq("User:Foo"), eq("ResourceOwner"), eq(project.namePrefix()));
  }
}
