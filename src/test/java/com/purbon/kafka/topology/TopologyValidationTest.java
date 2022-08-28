package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TopologyValidationTest {

  private TopologySerdes parser;

  @BeforeEach
  public void setup() {
    parser = new TopologySerdes();
  }

  @Test
  void positiveExecutionOnCamelCaseNames() {

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    Configuration config =
        createTopologyBuilderConfig(
            "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).isEmpty();
  }

  @Test
  void invalidExecutionBecauseOfNumberOfPartitions() {

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-camelCaseNames.yml"));

    Configuration config =
        createTopologyBuilderConfig(
            "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation",
            "com.purbon.kafka.topology.validation.topic.PartitionNumberValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(0);
  }

  @Test
  void invalidExecutionWithFailedValidation() {

    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    Configuration config =
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
  void regexpValidationShouldFindPatterns() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    Configuration config =
        createTopologyBuilderConfig(
            "com.purbon.kafka.topology.validation.topology.CamelCaseNameFormatValidation",
            "com.purbon.kafka.topology.validation.topic.TopicNameRegexValidation");

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(1);
  }

  @Test
  void usingDeprecatedValidationsConfig() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

          Configuration config =
              createTopologyBuilderConfig("topology.CamelCaseNameFormatValidation");

          TopologyValidator validator = new TopologyValidator(config);
          List<String> results = validator.validate(topology);
          assertThat(results).hasSize(1);
          assertThat(results.get(0))
              .isEqualTo("Project name does not follow the camelCase format: foo");
        });
  }

  @Test
  void usingUnknownClassName() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

          Configuration config = createTopologyBuilderConfig("not.available.Validation");

          TopologyValidator validator = new TopologyValidator(config);
          validator.validate(topology);
        });
  }

  private Configuration createTopologyBuilderConfig(String... validations) {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_VALIDATIONS_CONFIG, Arrays.asList(validations));
    props.put(TOPOLOGY_VALIDATIONS_TOPIC_NAME_REGEXP, "[a-zA-Z0-9.-]*");
    return new Configuration(cliOps, props);
  }
}
