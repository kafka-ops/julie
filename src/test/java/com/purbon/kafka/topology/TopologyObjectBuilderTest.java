package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.CommandLineInterface.RECURSIVE_OPTION;
import static com.purbon.kafka.topology.Constants.JULIE_ENABLE_MULTIPLE_CONTEXT_PER_DIR;
import static com.purbon.kafka.topology.Constants.PLATFORM_SERVERS_CONNECT;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.Test;

public class TopologyObjectBuilderTest {

  @Test
  public void buildTopicNameTest() throws IOException {
    String fileOrDirPath = TestUtils.getResourceFilename("/dir");

    var map = TopologyObjectBuilder.build(fileOrDirPath);
    assertThat(map).hasSize(1);
    for (var entry : map.entrySet()) {
      assertThat(entry.getValue().getProjects()).hasSize(4);
    }
  }

  @Test
  public void buildOutOfMultipleTopos() throws IOException {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    var props = new Properties();
    props.put(JULIE_ENABLE_MULTIPLE_CONTEXT_PER_DIR, "true");
    Configuration config = new Configuration(cliOps, props);

    String fileOrDirPath = TestUtils.getResourceFilename("/dir_with_multiple");
    var map = TopologyObjectBuilder.build(fileOrDirPath, config);
    assertThat(map).hasSize(2);
    for (var entry : map.entrySet()) {
      assertThat(entry.getValue().getProjects()).hasSize(4);
    }

    var contextTopology = map.get("contextOrg");
    assertThat(contextTopology.getOrder().get(0)).isEqualTo("source");
    assertThat(contextTopology.getOrder().get(1)).isEqualTo("foo");
    assertThat(contextTopology.getOrder().get(2)).isEqualTo("bar");
    assertThat(contextTopology.getOrder().get(3)).isEqualTo("zet");

    var context = map.get("context2");
    assertThat(context.getOrder()).hasSize(3);
    assertThat(context.getOrder().get(0)).isEqualTo("source2");
    assertThat(context.getOrder().get(1)).isEqualTo("foo2");
    assertThat(context.getOrder().get(2)).isEqualTo("bar2");

    var projects =
        Arrays.asList(
            "context2.source2.foo.bar.projectFoo.",
            "context2.source2.foo.bar.projectBar.",
            "context2.source2.foo.bar.projectZet.",
            "context2.source2.foo.bar.projectBear.");
    assertThat(context.getProjects()).hasSize(4);
    for (Project proj : context.getProjects()) {
      assertThat(projects).contains(proj.namePrefix());
    }
  }

  @Test(expected = IOException.class)
  public void buildOutOfMultipleToposIfNotEnabled() throws IOException {
    String fileOrDirPath = TestUtils.getResourceFilename("/dir_with_multiple");
    TopologyObjectBuilder.build(fileOrDirPath);
  }

  @Test(expected = IOException.class)
  public void shouldRaiseAnExceptionIfTryingToParseMultipleTopologiesWithSharedProjects()
      throws IOException {
    String fileOrDirPath = TestUtils.getResourceFilename("/dir_with_prob");
    TopologyObjectBuilder.build(fileOrDirPath);
  }

  @Test
  public void buildOnlyConnectorTopo() throws IOException {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:http://foo:8083");

    Configuration config = new Configuration(cliOps, props);
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor-only-conn.yaml");
    var topologies = TopologyObjectBuilder.build(fileOrDirPath, config);
    var topology = topologies.values().stream().findFirst().get();

    var proj = topology.getProjects().get(0);
    assertThat(proj.getConnectorArtefacts().getConnectors()).hasSize(2);
  }

  @Test
  public void buildTopoOnlyPlatform() throws IOException {
    String fileOrDirPath = TestUtils.getResourceFilename("/descriptor-only-platform.yaml");
    var topologies = TopologyObjectBuilder.build(fileOrDirPath);
    var topology = topologies.values().stream().findFirst().get();
    assertThat(topology.getProjects()).hasSize(0);
    assertThat(topology.getPlatform().getKafka().getRbac()).isPresent();
    assertThat(topology.getPlatform().getKafka().getRbac().get()).hasSize(2);
  }

  @Test
  public void testConfigUpdateWhenUsingCustomPlans() throws IOException {
    String descriptorFile = TestUtils.getResourceFilename("/descriptor-with-plans.yaml");
    String plansFile = TestUtils.getResourceFilename("/plans.yaml");

    Map<String, Topology> topologies = TopologyObjectBuilder.build(descriptorFile, plansFile);
    var topology = topologies.values().stream().findFirst().get();
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
    assertThat(topic.getConfig()).containsEntry("bar", "1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTopologyWithPlansButWithNoPlansDef() throws IOException {
    String descriptorFile = TestUtils.getResourceFilename("/descriptor-with-plans.yaml");
    TopologyObjectBuilder.build(descriptorFile);
  }

  @Test(expected = TopologyParsingException.class)
  public void testTopologyWithInvalidPlan() throws IOException {
    String descriptorFile = TestUtils.getResourceFilename("/descriptor-with-invalid-plan.yaml");
    String plansFile = TestUtils.getResourceFilename("/plans.yaml");
    TopologyObjectBuilder.build(descriptorFile, plansFile);
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

  @Test
  public void shouldReadFilesRecursively() throws IOException {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(RECURSIVE_OPTION, "true");
    var props = new Properties();
    Configuration config = new Configuration(cliOps, props);

    String fileOrDirPath = TestUtils.getResourceFilename("/dir_recursive");
    var map = TopologyObjectBuilder.build(fileOrDirPath, config);
    assertThat(map).hasSize(1);
    final Topology topology = map.values().iterator().next();
    assertThat(topology.getProjects()).hasSize(4);
  }
}
