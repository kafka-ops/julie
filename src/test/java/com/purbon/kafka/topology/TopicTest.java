package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
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
    topology = new TopologyImpl();
    topology.setTeam("team");

    project = new ProjectImpl();
    project.setTopologyPrefix(topology.buildNamePrefix());

    project.setName("project");
    topology.setProjects(Arrays.asList(project));
  }

  @Test
  public void buildTopicNameTest() {
    Topic topic = new Topic("topic");
    topic.setProjectPrefix(project.buildTopicPrefix());
    String fulllName = topic.toString();
    Assert.assertEquals("team.project.topic", fulllName);
  }

  @Test
  public void buildTopicNameWithOtherDataPointsTest() {

    Topology topology = new TopologyImpl();
    topology.setTeam("team");

    topology.addOther("other-f", "other");
    topology.addOther("another-f", "another");

    Project project = new ProjectImpl();
    project.setTopologyPrefix(topology.buildNamePrefix());

    project.setName("project");
    topology.setProjects(Arrays.asList(project));

    Topic topic = new Topic("topic");
    topic.setProjectPrefix(project.buildTopicPrefix());
    String fulllName = topic.toString();
    Assert.assertEquals("team.other.another.project.topic", fulllName);
  }

  @Test
  public void buildTopicNameWithDataTypeTest() {
    Topic topic = new Topic("topic", "type");
    topic.setProjectPrefix(project.buildTopicPrefix());
    String fulllName = topic.toString();
    Assert.assertEquals("team.project.topic.type", fulllName);
  }
}
