package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopicTest {

  Topology topology;
  Project project;

  @Before
  public void before() {
    topology = new Topology();
    project = new Project();
    project.setTopology(topology);
    topology.setSource("source");
    topology.setTeam("team");

    project.setName("project");
    topology.setProjects(Arrays.asList(project));
  }

  @Test
  public void buildTopicNameTest() {
    Topic topic = new Topic("topic");
    topic.setProject(project);
    String fulllName = topic.toString();
    Assert.assertEquals("team.source.project.topic", fulllName);
  }

  @Test
  public void buildTopicNameWithDataTypeTest() {
    Topic topic = new Topic("topic", "type");
    topic.setProject(project);
    String fulllName = topic.toString();
    Assert.assertEquals("team.source.project.topic.type", fulllName);
  }

}
