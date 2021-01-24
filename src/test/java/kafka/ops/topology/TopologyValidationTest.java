package kafka.ops.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.*;
import kafka.ops.topology.model.Topology;
import kafka.ops.topology.serdes.TopologySerdes;
import kafka.ops.topology.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

public class TopologyValidationTest {

  private TopologySerdes parser;

  @Before
  public void setup() {
    parser = new TopologySerdes();
  }

  @Test
  public void testPositiveExecutionOnCamelCaseNames() throws IOException {

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    cliOps.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TopologyBuilderConfig.TOPOLOGY_VALIDATIONS_CONFIG,
        Arrays.asList("topology.CamelCaseNameFormatValidation"));
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).isEmpty();
  }

  @Test
  public void testInvalidExecutionBecuaseofNumberOfPartitions() throws IOException {

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    cliOps.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TopologyBuilderConfig.TOPOLOGY_VALIDATIONS_CONFIG,
        Arrays.asList("topology.CamelCaseNameFormatValidation", "topic.PartitionNumberValidation"));
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(0);
  }

  @Test
  public void testInvalidExecutionWithFailedValidation() throws IOException {

    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    cliOps.put(BuilderCli.ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TopologyBuilderConfig.TOPOLOGY_VALIDATIONS_CONFIG,
        Arrays.asList("topology.CamelCaseNameFormatValidation", "topic.PartitionNumberValidation"));
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(5);
    assertThat(results.get(0)).isEqualTo("Project name does not follow the camelCase format: foo");
    assertThat(results.get(1))
        .isEqualTo("Topic contextOrg.source.foo.foo has an invalid number of partitions: 1");
    assertThat(results.get(2))
        .isEqualTo("Topic contextOrg.source.bar.bar.avro has an invalid number of partitions: 1");
    assertThat(results.get(3))
        .isEqualTo("Topic contextOrg.source.baz.topicE has an invalid number of partitions: 1");
    assertThat(results.get(4))
        .isEqualTo("Topic contextOrg.source.baz.topicF has an invalid number of partitions: 1");
  }
}
