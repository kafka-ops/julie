package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.CommandLineInterface.CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.api.ksql.KsqlClientConfig;
import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.JulieRole;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.schema.TopicSchemas;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationTest {

  private Map<String, String> cliOps;
  private Properties props;

  @BeforeEach
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  void withAllRequiredFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();

    props.put(ACCESS_CONTROL_IMPLEMENTATION_CLASS, RBAC_ACCESS_CONTROL_CLASS);
    props.put(MDS_SERVER, "example.com");
    props.put(MDS_USER_CONFIG, "foo");
    props.put(MDS_PASSWORD_CONFIG, "bar");
    props.put(MDS_KAFKA_CLUSTER_ID_CONFIG, "1234");

    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void schemaRegistryConfigFields() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          Topology topology = new TopologyImpl();
          Project project = new ProjectImpl();
          Topic topic = new Topic();
          TopicSchemas schema = new TopicSchemas("foo", "bar");
          topic.setSchemas(Collections.singletonList(schema));
          project.addTopic(topic);
          topology.addProject(project);
          props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "mock://");

          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void schemaRegistryValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new Topic();
    TopicSchemas schema = new TopicSchemas("foo", "bar");
    topic.setSchemas(Collections.singletonList(schema));
    project.addTopic(topic);
    topology.addProject(project);

    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void schemaRegistryValidConfigButNoSchemas() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new Topic();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(CONFLUENT_SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void noSchemaRegistry() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    Topic topic = new Topic("topic", "json");
    project.addTopic(topic);
    topology.addProject(project);

    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void prefixValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new Topic();
    project.addTopic(topic);
    topology.addProject(project);

    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");
    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{foo}}");

    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void missingPrefixValidConfigFields() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          Topology topology = new TopologyImpl();
          Project project = new ProjectImpl();
          Topic topic = new Topic();
          project.addTopic(topic);
          topology.addProject(project);

          props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");

          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void missingTopicPrefixValidConfigFields() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          Topology topology = new TopologyImpl();
          Project project = new ProjectImpl();
          Topic topic = new Topic();
          project.addTopic(topic);
          topology.addProject(project);

          props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{foo}}{{topic}}");

          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void incompatiblePrefixValidConfigFields() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          Topology topology = new TopologyImpl();
          Project project = new ProjectImpl();
          Topic topic = new Topic();
          project.addTopic(topic);
          topology.addProject(project);

          props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{banana}}");

          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void kafkaInternalTopicExtendedPrefix() {
    String clientConfigFile =
        TestUtils.getResourceFilename("/config-internals-extended.properties");

    cliOps.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    Configuration config = Configuration.build(cliOps);
    assertThat(config.getKafkaInternalTopicPrefixes())
        .isEqualTo(Arrays.asList("_", "topicA", "topicB"));
  }

  @Test
  void ksqlServerWithHttps() {
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");
    cliOps.put(CLIENT_CONFIG_OPTION, clientConfigFile);
    props.put(PLATFORM_SERVER_KSQL_URL, "https://example.com:8083");
    Configuration config = new Configuration(cliOps, props);
    KsqlClientConfig ksqlClientConfig = config.getKSQLClientConfig();

    assertThat(ksqlClientConfig.getServer().getProtocol()).isEqualTo("https");
    assertThat(ksqlClientConfig.getServer().getHost()).isEqualTo("example.com");
    assertThat(ksqlClientConfig.getServer().getPort()).isEqualTo(8083);
  }

  @Test
  void ksqlServerWithoutScheme() {
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");
    cliOps.put(CLIENT_CONFIG_OPTION, clientConfigFile);
    props.put(PLATFORM_SERVER_KSQL_URL, "example.com:8083");
    Configuration config = new Configuration(cliOps, props);

    assertThatThrownBy(config::getKSQLClientConfig)
        .hasMessageContaining("example.com:8083")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void julieRolesFetch() throws IOException {

    String rolesFile = TestUtils.getResourceFilename("/roles.yaml");
    props.put(JULIE_ROLES, rolesFile);
    Configuration config = new Configuration(cliOps, props);

    var roles = config.getJulieRoles();
    assertThat(roles).isNotNull();
    assertThat(roles.getRoles()).hasSize(2);
    for (JulieRole role : roles.getRoles()) {
      assertThat(role.getName()).isIn("app", "other");
    }

    JulieRole role = roles.get("app");
    List<String> resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic", "Group");
    assertThat(role.getName()).isEqualTo("app");
    assertThat(role.getAcls()).hasSize(4);

    role = roles.get("other");
    resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic");
    assertThat(role.getName()).isEqualTo("other");
    assertThat(role.getAcls()).hasSize(2);
  }

  @Test
  void wrongFileJulieRoles() throws IOException {
    assertThrows(
        IOException.class,
        () -> {
          String rolesFile = TestUtils.getResourceFilename("/descriptor.yaml");
          props.put(JULIE_ROLES, rolesFile);
          Configuration config = new Configuration(cliOps, props);
          config.getJulieRoles();
        });
  }

  @Test
  void julieOpsInstanceIdGeneration() {
    props.put(JULIE_INSTANCE_ID, "12345");
    Configuration config = new Configuration(cliOps, props);
    assertThat(config.getJulieInstanceId()).isEqualTo("12345");
  }

  @Test
  void randomJulieOpsInstanceIdGeneration() {
    Configuration config = new Configuration(cliOps, props);
    assertThat(config.getJulieInstanceId()).hasSize(10);
    assertThat(config.getJulieInstanceId())
        .has(
            new Condition<>() {
              @Override
              public boolean matches(String value) {
                return value.chars().allMatch(value1 -> (97 <= value1) && (value1 <= 122));
              }
            });
  }

  @Test
  void kafkaInternalTopicDefaultPrefix() {
    String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

    cliOps.put(CLIENT_CONFIG_OPTION, clientConfigFile);

    Configuration config = Configuration.build(cliOps);
    assertThat(config.getKafkaInternalTopicPrefixes()).isEqualTo(Collections.singletonList("_"));
  }

  @Test
  void shouldAddStreamsApplicationIdAsInternalTopics() {
    Configuration config = new Configuration(cliOps, props);

    var topology =
        TestTopologyBuilder.createProject().addKStream("foo", "applicationId").buildTopology();

    var internals = config.getKafkaInternalTopicPrefixes(Collections.singletonList(topology));
    assertThat(internals).contains("applicationId");
    assertThat(internals).contains("_");
  }

  @Test
  void shouldAddStreamsProjectPrefixAsInternalTopics() {
    Configuration config = new Configuration(cliOps, props);

    var topology = TestTopologyBuilder.createProject().addKStream("foo").buildTopology();

    var internals = config.getKafkaInternalTopicPrefixes(Collections.singletonList(topology));
    assertThat(internals).contains("ctx.project.");
    assertThat(internals).contains("_");
  }

  @Test
  void shouldFetchAConfigSubsetSuccessfully() {
    props.put(JULIE_AUDIT_APPENDER_CLASS, "foo.class");
    props.put(AUDIT_APPENDER_KAFKA_TOPIC, "log");

    Configuration config = new Configuration(cliOps, props);
    Properties props = config.asProperties("julie.audit");
    assertThat(props).hasSize(5);
    assertThat(props).containsEntry(AUDIT_APPENDER_KAFKA_TOPIC, "log");
    assertThat(props).containsEntry(JULIE_AUDIT_APPENDER_CLASS, "foo.class");
  }

  @Test
  void nonEmptyTopicManagedPrefixConfigsShouldValidateSuccessfully() throws ConfigurationException {
    var topology = TestTopologyBuilder.createProject().buildTopology();
    props.put(TOPIC_MANAGED_PREFIXES + ".0", "foo");
    Configuration config = new Configuration(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  void emptyTopicManagedPrefixConfigsShouldRaiseAnError() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          var topology = TestTopologyBuilder.createProject().buildTopology();
          props.put(TOPIC_MANAGED_PREFIXES + ".0", "");
          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void emptyGroupManagedPrefixConfigsShouldRaiseAnError() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          var topology = TestTopologyBuilder.createProject().buildTopology();
          props.put(GROUP_MANAGED_PREFIXES + ".0", "");
          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }

  @Test
  void emptySaManagedPrefixConfigsShouldRaiseAnError() throws ConfigurationException {
    assertThrows(
        ConfigurationException.class,
        () -> {
          var topology = TestTopologyBuilder.createProject().buildTopology();
          props.put(SERVICE_ACCOUNT_MANAGED_PREFIXES + ".0", "");
          Configuration config = new Configuration(cliOps, props);
          config.validateWith(topology);
        });
  }
}
