package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPOLOGY_VALIDATIONS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

public class TopologyValidationTest {

  private TopologySerdes parser;

  @Before
  public void setup() {
    parser = new TopologySerdes();
  }

  @Test
  public void testPositiveExecutionOnCamelCaseNames() {

    Topology topology =
            parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    TopologyBuilderConfig config =
            createTopologyBuilderConfig(
                    "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).isEmpty();
  }

  @Test
  public void testInvalidExecutionBecuaseofNumberOfPartitions() {

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    TopologyBuilderConfig config =
        createTopologyBuilderConfig(
                "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation",
                "com.purbon.kafka.topology.validation.topic.PartitionNumberValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(0);
  }

  @Test
  public void testInvalidExecutionWithFailedValidation() {

    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    TopologyBuilderConfig config =
        createTopologyBuilderConfig(
                "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation",
                "com.purbon.kafka.topology.validation.topic.PartitionNumberValidation");

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

  @Test
  public void testUsingDeprecatedValidationsConfig() {

    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    TopologyBuilderConfig config =
            createTopologyBuilderConfig(
                    "topology.CamelCaseNameFormatValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(1);
    assertThat(results.get(0)).isEqualTo("Project name does not follow the camelCase format: foo");
  }

  @Test(expected = IllegalStateException.class)
  public void testUsingUnknwownClassName() {

    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    TopologyBuilderConfig config = createTopologyBuilderConfig("not.available.Validation");

    TopologyValidator validator = new TopologyValidator(config);
    validator.validate(topology);
  }

  private TopologyBuilderConfig createTopologyBuilderConfig(String... validations) {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_VALIDATIONS_CONFIG, Arrays.asList(validations));
    return new TopologyBuilderConfig(cliOps, props);
  }
}
