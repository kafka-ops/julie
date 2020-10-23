package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.PROJECT_PREFIX_FORMAT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPIC_PREFIX_FORMAT_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.Topology;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class TopologyBuilderConfigTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  public void testWithAllRequiredFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();

    props.put(ACCESS_CONTROL_IMPLEMENTATION_CLASS, TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS);
    props.put(MDS_SERVER, "example.com");
    props.put(MDS_USER_CONFIG, "foo");
    props.put(MDS_PASSWORD_CONFIG, "bar");
    props.put(MDS_KAFKA_CLUSTER_ID_CONFIG, "1234");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test(expected = ConfigurationException.class)
  public void testSchemaRegistryConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    TopicSchemas schema = new TopicSchemas("foo", "bar");
    topic.setSchemas(schema);
    project.addTopic(topic);
    topology.addProject(project);
    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "mock://");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testSchemaRegistryValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    TopicSchemas schema = new TopicSchemas("foo", "bar");
    topic.setSchemas(schema);
    project.addTopic(topic);
    topology.addProject(project);

    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testSchemaRegistryValidConfigButNoSchemas() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testPrefixValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");
    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{foo}}");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingPrefixValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingTopicPrefixValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test(expected = ConfigurationException.class)
  public void testIncompatiblePrefixValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");
    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{banana}}");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testKafkaInternalTopicDefaultPrefix() throws URISyntaxException {
    URL clientConfigURL = getClass().getResource("/client-config.properties");
    String clientConfigFile = Paths.get(clientConfigURL.toURI()).toFile().toString();

    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    TopologyBuilderConfig config = TopologyBuilderConfig.build(cliOps);
    assertThat(config.getKafkaInternalTopicPrefixes()).isEqualTo(Arrays.asList("_"));
  }

  @Test
  public void testKafkaInternalTopicExtendedPrefix() throws URISyntaxException {
    URL clientConfigURL = getClass().getResource("/config-internals-extended.properties");
    String clientConfigFile = Paths.get(clientConfigURL.toURI()).toFile().toString();

    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, clientConfigFile);

    TopologyBuilderConfig config = TopologyBuilderConfig.build(cliOps);
    assertThat(config.getKafkaInternalTopicPrefixes())
        .isEqualTo(Arrays.asList("_", "topicA", "topicB"));
  }
}
