package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class TopologyObjectBuilderTest {

  @Test
  public void buildTopicNameTest() throws URISyntaxException, IOException {

    URL dirOfDescriptors = getClass().getResource("/dir");
    String fileOrDirPath = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    Topology topology = TopologyObjectBuilder.build(fileOrDirPath);

    assertEquals(4, topology.getProjects().size());
  }

  @Test
  public void testConfigUpdateWhenUsingCustomPlans() throws URISyntaxException, IOException {
    URL dirOfDescriptors = getClass().getResource("/descriptor-with-plans.yaml");
    String descriptorFile = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    URL plansFileURL = getClass().getResource("/plans.yaml");
    String plansFile = Paths.get(plansFileURL.toURI()).toFile().toString();

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

    // should overwide values with priority given to the plans
    topic = map.get("barFooBar");
    assertThat(topic.getConfig()).containsEntry("replication.factor", "1");
    assertThat(topic.getConfig()).containsEntry("bar", "3");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTopologyWithPlansButWithNoPlansDef() throws URISyntaxException, IOException {
    URL dirOfDescriptors = getClass().getResource("/descriptor-with-plans.yaml");
    String descriptorFile = Paths.get(dirOfDescriptors.toURI()).toFile().toString();
    TopologyObjectBuilder.build(descriptorFile);
  }
}
