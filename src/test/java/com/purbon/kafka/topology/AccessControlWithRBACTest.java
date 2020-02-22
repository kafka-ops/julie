package com.purbon.kafka.topology;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.roles.RBACProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlWithRBACTest {

  @Mock
  RBACProvider aclsProvider;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();


  private AccessControlManager accessControlManager;

  @Before
  public void setup() {
    accessControlManager = new AccessControlManager(aclsProvider);
  }

  @Test
  public void testPredefinedRoles() {

    Map<String, List<String>>  predefinedRoles = new HashMap<>();
    predefinedRoles
        .put("ResourceOwner", Arrays.asList("User:Foo"));

    Project project = new Project();
    project.setRbacRawRoles(predefinedRoles);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);

    Topology topology = new Topology();
    topology.addProject(project);

    doNothing()
        .when(aclsProvider)
        .setPredefinedRole("User:Foo", "ResourceOwner", project.buildTopicPrefix());

    accessControlManager.sync(topology);

    verify(aclsProvider, times(1))
        .setPredefinedRole(eq("User:Foo"), eq("ResourceOwner"), eq(project.buildTopicPrefix()));


  }

}

