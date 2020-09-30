package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPIC_PREFIX_FORMAT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPIC_PREFIX_SEPARATOR_CONFIG;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopicTest {

  Topology topology;
  Project project;

  @Before
  public void before() {
    topology = new TopologyImpl();
    topology.setContext("team");
    project = new ProjectImpl("project");
    topology.setProjects(Arrays.asList(project));
  }

  @Test
  public void buildTopicNameTest() {
    Topic topic = new TopicImpl("topic");
    topic.setDefaultProjectPrefix(project.namePrefix());
    String fulllName = topic.toString();
    Assert.assertEquals("team.project.topic", fulllName);
  }

  @Test
  public void buildTopicNameWithOtherDataPointsTest() {

    Topology topology = new TopologyImpl();
    topology.setContext("team");

    topology.addOther("other-f", "other");
    topology.addOther("another-f", "another");

    Project project = new ProjectImpl("project");
    topology.addProject(project);

    topology.setProjects(Collections.singletonList(project));

    Topic topic = new TopicImpl("topic");
    project.addTopic(topic);
    String fulllName = topic.toString();
    Assert.assertEquals("team.other.another.project.topic", fulllName);
  }

  @Test
  public void buildTopicNameWithDataTypeTest() {
    Topic topic = new TopicImpl("topic", "type");
    topic.setDefaultProjectPrefix(project.namePrefix());
    String fulllName = topic.toString();
    Assert.assertEquals("team.project.topic.type", fulllName);
  }

  @Test
  public void buildTopicNameFormatWithCustomSeparator() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_SEPARATOR_CONFIG, "_");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    Topology topology = new TopologyImpl(config);
    topology.setContext("team");

    topology.addOther("other-f", "other");
    topology.addOther("another-f", "another");

    Project project = new ProjectImpl("project", config);
    topology.setProjects(Collections.singletonList(project));

    Topic topic = new TopicImpl("topic", config);
    project.addTopic(topic);

    String fulllName = topic.toString();
    Assert.assertEquals("team_other_another_project_topic", fulllName);
  }

  @Test
  public void buildTopicNameFormatWithCustomPattern() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{otherf}}.{{context}}.{{project}}.{{topic}}");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    Topology topology = new TopologyImpl(config);
    topology.setContext("team");

    topology.addOther("otherf", "other");
    topology.addOther("anotherf", "another");

    Project project = new ProjectImpl("project", config);
    topology.setProjects(Collections.singletonList(project));

    Topic topic = new TopicImpl("topic", config);
    project.addTopic(topic);

    String fulllName = topic.toString();
    Assert.assertEquals("other.team.project.topic", fulllName);
  }
}
