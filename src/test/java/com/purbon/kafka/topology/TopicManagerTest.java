package com.purbon.kafka.topology;

import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TopicManagerTest {

  @Mock
  TopologyBuilderAdminClient adminClient;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();


  private TopicManager topicManager;

  @Before
  public void setup() {
    topicManager = new TopicManager(adminClient);
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
    topicManager.syncTopics(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.composeTopicName(topology, project.getName()));
    verify(adminClient, times(1)).createTopic(topicB, topicB.composeTopicName(topology, project.getName()));
  }

  @Test
  public void newTopicUpdateTest() {

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);
    Topology topology = new Topology();
    topology.addProject(project);

    Set<String> dummyTopicList = new HashSet<>();
    dummyTopicList.add(topicB.composeTopicName(topology, project.getName()));
    when(adminClient.listTopics()).thenReturn(dummyTopicList);

    topicManager.syncTopics(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.composeTopicName(topology, project.getName()));
    verify(adminClient, times(1)).updateTopicConfig(topicB, topicB.composeTopicName(topology, project.getName()));
  }

  @Test
  public void newTopicDeleteTest() {

    Topic topicC = new Topic("topicC");

    Project project = new Project("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);
    Topology topology = new Topology();
    topology.addProject(project);

    Set<String> dummyTopicList = new HashSet<>();
    String topicCFullName = topicC.composeTopicName(topology, project.getName());
    dummyTopicList.add(topicCFullName);
    when(adminClient.listTopics()).thenReturn(dummyTopicList);

    topicManager.syncTopics(topology);

    verify(adminClient, times(1)).createTopic(topicA, topicA.composeTopicName(topology, project.getName()));
    verify(adminClient, times(1)).createTopic(topicB, topicB.composeTopicName(topology, project.getName()));
    verify(adminClient, times(1)).deleteTopics(Collections.singletonList(topicCFullName));
  }
}
