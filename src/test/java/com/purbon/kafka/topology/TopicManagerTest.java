package com.purbon.kafka.topology;

import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TopicManagerTest {

  @Mock TopologyBuilderAdminClient adminClient;

  @Mock SchemaRegistryManager schemaRegistryManager;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private TopicManager topicManager;

  @Before
  public void setup() {
    topicManager = new TopicManager(adminClient, schemaRegistryManager);
  }

  @Test
  public void newTopicCreationTest() {

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);
    Topology topology = new Topology();
    topology.addProject(project);

    when(adminClient.listTopics()).thenReturn(new HashSet<>());
    topicManager.sync(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
  }

  @Test
  public void topicUpdateTest() {

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);
    Topology topology = new Topology();
    topology.addProject(project);

    Set<String> dummyTopicList = new HashSet<>();
    dummyTopicList.add(topicB.toString());
    when(adminClient.listTopics()).thenReturn(dummyTopicList);

    topicManager.sync(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).updateTopicConfig(topicB, topicB.toString());
  }

  @Test
  public void topicDeleteTest() {

    // Original Topology
    Topology topology0 = new Topology();
    Project project0 = new Project("project");
    topology0.addProject(project0);

    Topic topicC = new Topic("topicC");
    project0.addTopic(topicC);

    // Topology after delete action
    Topology topology = new Topology();
    Project project = new Project("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    Set<String> dummyTopicList = new HashSet<>();
    String topicCFullName = topicC.toString();
    dummyTopicList.add(topicCFullName);
    when(adminClient.listTopics()).thenReturn(dummyTopicList);

    topicManager.sync(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
    verify(adminClient, times(1)).deleteTopics(Collections.singletonList(topicCFullName));
  }
}
