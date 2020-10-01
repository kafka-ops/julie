package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPOLOGY_VALIDATIONS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class TopologyValidationTest {

  private TopologySerdes parser;

  @Before
  public void setup() {
    parser = new TopologySerdes();
  }

  @Test
  public void testPositiveExecutionOnCamelCaseNames() throws IOException, URISyntaxException {

    URL descriptorWithOptionals = getClass().getResource("/descriptor-with-camelCaseNames.yml");
    Topology topology = parser.deserialise(Paths.get(descriptorWithOptionals.toURI()).toFile());

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_VALIDATIONS_CONFIG, "topology.CamelCaseNameFormatValidation");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).isEmpty();
  }

  @Test
  public void testInvalidExecutionBecuaseofNumberOfPartitions()
      throws IOException, URISyntaxException {

    URL descriptorWithOptionals = getClass().getResource("/descriptor-with-camelCaseNames.yml");
    Topology topology = parser.deserialise(Paths.get(descriptorWithOptionals.toURI()).toFile());

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TOPOLOGY_VALIDATIONS_CONFIG,
        "topology.CamelCaseNameFormatValidation, topic.PartitionNumberValidation");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(0);
  }

  @Test
  public void testInvalidExecutionWithFailedValidation()
      throws IOException, URISyntaxException {

    URL descriptorWithOptionals = getClass().getResource("/descriptor.yaml");
    Topology topology = parser.deserialise(Paths.get(descriptorWithOptionals.toURI()).toFile());

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TOPOLOGY_VALIDATIONS_CONFIG,
        "topology.CamelCaseNameFormatValidation, topic.PartitionNumberValidation");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologyValidator validator = new TopologyValidator(config);
    List<String> results = validator.validate(topology);
    assertThat(results).hasSize(3);
    assertThat(results.get(0)).isEqualTo("Project name does not follow the camelCase format: foo");
    assertThat(results.get(1)).isEqualTo("Topic contextOrg.source.foo.foo has an invalid number of partitions: 1");
    assertThat(results.get(2)).isEqualTo("Topic contextOrg.source.bar.bar.avro has an invalid number of partitions: 1");
  }
}
