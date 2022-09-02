package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TopicTest {

  Topology topology;
  Project project;

  @BeforeEach
  void before() {
    topology = new TopologyImpl();
    topology.setContext("team");
    project = new ProjectImpl("project");
    topology.setProjects(Arrays.asList(project));
  }

  @Test
  void buildTopicNameTest() {
    Topic topic = new Topic("topic");
    topic.setProjectPrefix(project.namePrefix());
    String fullName = topic.toString();
    assertEquals("team.project.topic", fullName);
  }

  @Test
  void buildTopicNameWithOtherDataPointsTest() {

    Topology topology = new TopologyImpl();
    topology.setContext("team");

    topology.addOther("other-f", "other");
    topology.addOther("another-f", "another");

    Project project = new ProjectImpl("project");
    topology.addProject(project);

    topology.setProjects(Collections.singletonList(project));

    Topic topic = new Topic("topic");
    project.addTopic(topic);
    String fullName = topic.toString();
    assertEquals("team.other.another.project.topic", fullName);
  }

  @Test
  void buildTopicNameWithDataTypeTest() {
    Topic topic = new Topic("topic", "type");
    topic.setProjectPrefix(project.namePrefix());
    String fullName = topic.toString();
    assertEquals("team.project.topic.type", fullName);
  }

  @Test
  void buildTopicNameFormatWithCustomSeparator() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_SEPARATOR_CONFIG, "_");
    Configuration config = new Configuration(cliOps, props);

    Topology topology = new TopologyImpl(config);
    topology.setContext("team");

    topology.addOther("other-f", "other");
    topology.addOther("another-f", "another");

    Project project = new ProjectImpl("project", config);
    topology.setProjects(Collections.singletonList(project));

    Topic topic = new Topic("topic", config);
    project.addTopic(topic);

    String fullName = topic.toString();
    assertEquals("team_other_another_project_topic", fullName);
  }

  @Test
  void buildTopicNameFormatWithCustomPattern() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{otherf}}.{{context}}.{{project}}.{{topic}}");
    Configuration config = new Configuration(cliOps, props);

    Topology topology = new TopologyImpl(config);
    topology.setContext("team");

    topology.addOther("otherf", "other");
    topology.addOther("anotherf", "another");

    Project project = new ProjectImpl("project", config);
    topology.setProjects(Collections.singletonList(project));

    Topic topic = new Topic("topic", config);
    project.addTopic(topic);

    String fullName = topic.toString();
    assertEquals("other.team.project.topic", fullName);
  }
}
