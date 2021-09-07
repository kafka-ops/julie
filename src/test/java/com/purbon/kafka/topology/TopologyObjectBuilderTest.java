package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class TopologyObjectBuilderTest {

  @Test
  public void buildTopicNameTest() throws IOException {
    String fileOrDirPath = TestUtils.getResourceFilename("/dir");

    Topology topology = TopologyObjectBuilder.build(fileOrDirPath);

    assertEquals(4, topology.getProjects().size());
  }

  @Test
  public void testConfigUpdateWhenUsingCustomPlans() throws IOException {
    String descriptorFile = TestUtils.getResourceFilename("/descriptor-with-plans.yaml");
    String plansFile = TestUtils.getResourceFilename("/plans.yaml");

    Topology topology = TopologyObjectBuilder.build(descriptorFile, plansFile);
    assertThat(topology).isNotNull();

    List<Topic> topics = topology.getProjects().get(0).getTopics();
    Map<String, Topic> map =
        topics.stream().collect(Collectors.toMap(Topic::getName, topic -> topic));

    // should include the config values from the plan into the topic config.
    Topic topic = map.get("fooBar");
    Map<String, String> config = new HashMap<>();
    config.put("foo", "bar");
    config.put("bar", "3");
    assertThat(topic.getConfig()).containsAllEntriesOf(config);

    // should respect values from the original config if not present in the plan description
    topic = map.get("barFoo");
    assertThat(topic.getConfig()).containsEntry("replication.factor", "1");

    // should override values with priority given to the plans
    topic = map.get("barFooBar");
    assertThat(topic.getConfig()).containsEntry("replication.factor", "1");
    assertThat(topic.getConfig()).containsEntry("bar", "3");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTopologyWithPlansButWithNoPlansDef() throws IOException {
    String descriptorFile = TestUtils.getResourceFilename("/descriptor-with-plans.yaml");
    TopologyObjectBuilder.build(descriptorFile);
  }

  @Test(expected = TopologyParsingException.class)
  public void testInvalidTopology() throws IOException {
    String descriptorFile =
        TestUtils.getResourceFilename("/errors_dir/descriptor-with-errors.yaml");
    TopologyObjectBuilder.build(descriptorFile);
  }

  @Test(expected = TopologyParsingException.class)
  public void testInvalidTopologyFromDir() throws IOException {
    String dirPath = TestUtils.getResourceFilename("/errors_dir");
    TopologyObjectBuilder.build(dirPath);
  }
}
