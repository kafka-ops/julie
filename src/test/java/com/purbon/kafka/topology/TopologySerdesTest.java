package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static com.purbon.kafka.topology.model.SubjectNameStrategy.TOPIC_NAME_STRATEGY;
import static com.purbon.kafka.topology.model.SubjectNameStrategy.TOPIC_RECORD_NAME_STRATEGY;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_READ;
import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.RESOURCE_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.model.artefact.KsqlArtefacts;
import com.purbon.kafka.topology.model.artefact.KsqlStreamArtefact;
import com.purbon.kafka.topology.model.artefact.KsqlTableArtefact;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
import com.purbon.kafka.topology.model.users.platform.KsqlServerInstance;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistryInstance;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes.FileType;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class TopologySerdesTest {

  private TopologySerdes parser;

  @Before
  public void setup() {
    Properties props = new Properties();
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:foo");
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    parser = new TopologySerdes(config, new PlanMap());
  }

  @Test
  public void testMetadata() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-metadata.yaml"));
    Project project = topology.getProjects().get(0);

    assertThat(project.getConsumers().get(0).getMetadata()).containsKey("system");
    assertThat(project.getProducers().get(0).getMetadata()).containsKey("contactInfo");
    assertThat(project.getStreams().get(0).getMetadata()).containsKey("system");
    assertThat(project.getConnectors().get(0).getMetadata()).containsKey("system");
    assertThat(project.getTopics().get(0).getMetadata()).containsKey("domain");
    assertThat(project.getTopics().get(1).getMetadata()).containsKey("domain");
    assertThat(project.getTopics().get(1).getMetadata()).containsKey("owner");
    assertThat(project.getTopics().get(0).getConsumers().get(0).getMetadata())
        .containsKey("system");
  }

  @Test
  public void testStreamsParsingOnlyReadTopicsShouldNotParseAsNull() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-streams-only-read.yaml"));

    Project p = topology.getProjects().get(0);

    for (KStream s : p.getStreams()) {
      assertThat(s.getTopics().get(KStream.READ_TOPICS)).isNotNull();
      assertThat(s.getTopics().get(KStream.READ_TOPICS)).isInstanceOf(List.class);
      assertThat(s.getTopics().get(KStream.READ_TOPICS)).contains("topicA", "topicB");
      assertThat(s.getTopics().get(KStream.WRITE_TOPICS)).isNotNull();
      assertThat(s.getTopics().get(KStream.WRITE_TOPICS)).isInstanceOf(List.class);
    }
  }

  @Test(expected = TopologyParsingException.class)
  public void testStreamsParsingOnlyWriteTopicsShoulRaiseAnException() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-streams-only-write.yaml"));
  }

  @Test
  public void testDynamicFirstLevelAttributes() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-others.yml"));
    Project project = topology.getProjects().get(0);
    assertThat(project.namePrefix()).startsWith("contextOrg.source.foo.bar.zet");

    Topology anotherTopology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project anotherProject = anotherTopology.getProjects().get(0);

    assertEquals("contextOrg.source.foo.", anotherProject.namePrefix());
  }

  @Test
  public void testFileWithoutTopicsShouldNotError() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-without-topics.yml"));
  }

  @Test
  public void testTopologySerialisation() throws IOException {
    Topology topology = new TopologyImpl();
    topology.setContext("contextOrg");
    topology.setProjects(buildProjects());

    String topologyYamlString = parser.serialise(topology);
    Topology deserTopology = parser.deserialise(topologyYamlString);

    assertEquals(topology.getContext(), deserTopology.getContext());
    assertEquals(topology.getProjects().size(), deserTopology.getProjects().size());
  }

  @Test
  public void testTopicConfigSerdes() throws IOException {
    Topology topology = new TopologyImpl();
    topology.setContext("team");

    HashMap<String, String> topicConfig = new HashMap<>();
    topicConfig.put("num.partitions", "1");
    topicConfig.put("replication.factor", "1");
    Topic topic = new Topic("foo", topicConfig);

    HashMap<String, String> topicBarConfig = new HashMap<>();
    topicBarConfig.put("num.partitions", "1");
    topicBarConfig.put("replication.factor", "1");
    Topic topicBar = new Topic("bar", "avro", topicBarConfig);

    Project project = new ProjectImpl("foo");

    KStream kstreamApp = new KStream();
    kstreamApp.setPrincipal("App0");
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    topics.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    kstreamApp.setTopics(topics);
    project.setStreams(Collections.singletonList(kstreamApp));

    Connector connector1 = new Connector();
    connector1.setPrincipal("Connect1");
    HashMap<String, List<String>> topics1 = new HashMap<>();
    topics1.put(KStream.READ_TOPICS, Arrays.asList("topicA", "topicB"));
    connector1.setTopics(topics1);

    Connector connector2 = new Connector();
    connector2.setPrincipal("Connect2");
    HashMap<String, List<String>> topics2 = new HashMap<>();
    topics2.put(KStream.WRITE_TOPICS, Arrays.asList("topicC", "topicD"));
    connector2.setTopics(topics2);
    project.setConnectors(Arrays.asList(connector1, connector2));

    Consumer consumer0 = new Consumer("app0");
    Consumer consumer1 = new Consumer("app1");
    project.setConsumers(Arrays.asList(consumer0, consumer1));

    project.setTopics(Arrays.asList(topic, topicBar));

    Project project2 = new ProjectImpl("bar");
    project2.setTopics(Arrays.asList(topicBar));

    topology.setProjects(Arrays.asList(project, project2));

    String topologyYamlString = parser.serialise(topology);
    Topology deserTopology = parser.deserialise(topologyYamlString);

    Project serdesProject = deserTopology.getProjects().get(0);
    Topic serdesTopic = serdesProject.getTopics().get(0);

    assertEquals(topic.getName(), serdesTopic.getName());
    assertEquals(topic.partitionsCount(), serdesTopic.partitionsCount());
  }

  @Test
  public void testTopicWithDataType() throws IOException {
    Project project = new ProjectImpl("foo");

    Topology topology = new TopologyImpl();
    topology.setContext("team");
    topology.addProject(project);

    HashMap<String, String> topicConfig = new HashMap<>();
    topicConfig.put("num.partitions", "3");
    topicConfig.put("replication.factor", "2");
    Topic topic = new Topic("foo", "json", topicConfig);
    project.addTopic(topic);

    Topic topic2 = new Topic("topic2", topicConfig);
    project.addTopic(topic2);

    String topologyYamlString = parser.serialise(topology);
    Topology deserTopology = parser.deserialise(topologyYamlString);

    Project serdesProject = deserTopology.getProjects().get(0);
    Topic serdesTopic = serdesProject.getTopics().get(0);

    assertEquals(topic.getDataType(), serdesTopic.getDataType());
    assertEquals(topic.getDataType().get(), serdesTopic.getDataType().get());

    Topic serdesTopic2 = serdesProject.getTopics().get(1);
    assertEquals(topic2.getDataType(), serdesTopic2.getDataType());
  }

  @Test(expected = TopologyParsingException.class)
  public void testTopologyWithNoTeam() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-with-no-context.yaml"));
  }

  @Test
  public void testTopologyWithNoProjectShouldNotError() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-with-no-project.yaml"));
  }

  @Test
  public void testCoreElementsProcessing() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    assertThat(topology.getProjects()).hasSize(3);

    Project project = topology.getProjects().get(0);
    assertThat(project.getProducers()).hasSize(3);
    assertThat(project.getConsumers()).hasSize(2);
    assertThat(project.getStreams()).hasSize(1);
    assertThat(project.getConnectors()).hasSize(2);

    assertThat(project.getProducers().get(0).getIdempotence()).isEmpty();
    assertThat(project.getProducers().get(1).getTransactionId()).isEqualTo(Optional.of("1234"));
    assertThat(project.getProducers().get(2).getIdempotence()).isNotEmpty();

    List<Topic> topics = topology.getProjects().get(2).getTopics();
    assertThat(topics).hasSize(2);
    assertThat(topics.get(0).toString()).isEqualTo("contextOrg.source.baz.topicE");
  }

  @Test
  public void testStreamsApps() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    Project project1 = topology.getProjects().get(0);
    assertThat(project1.getStreams()).hasSize(1);
    assertThat(project1.getStreams()).noneMatch(s -> s.getApplicationId().isPresent());

    Project project3 = topology.getProjects().get(2);
    assertThat(project3.getStreams()).hasSize(1);
    assertThat(project3.getStreams())
        .allMatch(s -> s.getApplicationId().orElse("notFound").equals("applicationId-1"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidSchemaSerdes() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-wrong-schemas.yaml"));
  }

  @Test
  public void testSchemaSerdes() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project project = topology.getProjects().get(0);
    List<Topic> topics = project.getTopics();
    Optional<Topic> topicBar = topics.stream().filter(t -> t.getName().equals("bar")).findFirst();
    Optional<Topic> topicCat = topics.stream().filter(t -> t.getName().equals("cat")).findFirst();

    assertThat(topicBar).isPresent();
    assertThat(topicBar.get().getSchemas()).hasSize(1);
    assertThat(topicBar.get().getSchemas().get(0).getValueSubject().hasSchemaFile()).isTrue();
    assertThat(topicBar.get().getSubjectNameStrategy()).isEqualTo(TOPIC_NAME_STRATEGY);

    assertThat(topicCat).isPresent();
    assertThat(topicCat.get().getSchemas()).hasSize(2);
    assertThat(topicCat.get().getSchemas().get(0).getValueSubject().hasSchemaFile()).isTrue();
    assertThat(topicCat.get().getSchemas().get(1).getValueSubject().hasSchemaFile()).isTrue();
    assertThat(topicCat.get().getSubjectNameStrategy()).isEqualTo(TOPIC_RECORD_NAME_STRATEGY);
  }

  @Test
  public void testOtherSerdes() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project project = topology.getProjects().get(0);

    var others = project.getOthers();
    assertThat(others).hasSize(2);
    var foos = others.get("app");
    assertThat(foos.get(0).getPrincipal()).isEqualTo("User:banana");
    assertThat(foos.get(0).groupString()).isEqualTo("foo");
    var bars = others.get("bar");
    assertThat(bars.get(0).getPrincipal()).isEqualTo("User:banana");
    assertThat(bars.get(0).groupString()).isEqualTo("bar");
  }

  @Test
  public void testKsqlSerdes() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project project = topology.getProjects().get(0);

    KSqlApp kSqlApp = project.getKSqls().get(0);
    assertThat(kSqlApp.getPrincipal()).isEqualTo("User:ksql0");
    assertThat(kSqlApp.getTopics().get("read")).hasSize(1);
    assertThat(kSqlApp.getTopics().get("write")).hasSize(1);

    KsqlArtefacts artefacts = project.getKsqlArtefacts();
    assertThat(artefacts.getStreams()).hasSize(1);
    KsqlStreamArtefact artefact = artefacts.getStreams().get(0);
    assertThat(artefact.getName()).isEqualTo("riderLocations");

    assertThat(artefacts.getTables()).hasSize(1);
    KsqlTableArtefact tableArtefact = artefacts.getTables().get(0);
    assertThat(tableArtefact.getName()).isEqualTo("users");
  }

  @Test
  public void testKsqlWithVarsSerdes() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project project = topology.getProjects().get(0);

    KsqlArtefacts artefacts = project.getKsqlArtefacts();
    assertThat(artefacts.getVars().getSessionVars()).hasSize(2);
    String firstVar = artefacts.getVars().getSessionVars().get("foo");
    assertThat(firstVar).isEqualTo("bar");
  }

  @Test
  public void testPlatformProcessing() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    assertEquals("contextOrg", topology.getContext());

    List<SchemaRegistryInstance> listOfSR =
        topology.getPlatform().getSchemaRegistry().getInstances();
    assertEquals(2, listOfSR.size());
    assertEquals("User:SchemaRegistry01", listOfSR.get(0).getPrincipal());
    assertEquals("foo", listOfSR.get(0).topicString());
    assertEquals("bar", listOfSR.get(0).groupString());
    assertEquals("User:SchemaRegistry02", listOfSR.get(1).getPrincipal());

    List<ControlCenterInstance> listOfC3 = topology.getPlatform().getControlCenter().getInstances();

    assertEquals(1, listOfC3.size());
    assertEquals("User:ControlCenter", listOfC3.get(0).getPrincipal());
    assertEquals("controlcenter", listOfC3.get(0).getAppId());

    List<KsqlServerInstance> listOfKsql = topology.getPlatform().getKsqlServer().getInstances();

    assertEquals(2, listOfKsql.size());
    assertEquals("User:ksql", listOfKsql.get(0).getPrincipal());
    assertEquals("ksql-server1", listOfKsql.get(0).getKsqlDbId());
    assertEquals("User:foo", listOfKsql.get(0).getOwner());

    assertEquals("User:ksql", listOfKsql.get(1).getPrincipal());
    assertEquals("ksql-server2", listOfKsql.get(1).getKsqlDbId());
    assertEquals("User:foo", listOfKsql.get(1).getOwner());
  }

  @Test
  public void testOnlyTopics() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));

    assertEquals("contextOrg", topology.getContext());
    assertTrue(topology.getProjects().get(0).getConnectors().isEmpty());
    assertTrue(topology.getProjects().get(0).getProducers().isEmpty());
    assertTrue(topology.getProjects().get(0).getStreams().isEmpty());
    assertTrue(topology.getProjects().get(0).getZookeepers().isEmpty());
  }

  @Test
  public void testOnlyConnectors() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-connector-alone.yaml"));

    var project = topology.getProjects().get(0);
    assertEquals("context", topology.getContext());
    assertTrue(project.getConnectors().isEmpty());
  }

  @Test
  public void testRBACTopics() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-rbac-topics.yaml"));

    Project project = topology.getProjects().get(0);
    assertEquals("contextOrg", topology.getContext());
    assertThat(project.getConnectors()).isEmpty();
    assertThat(project.getProducers()).isEmpty();
    assertThat(project.getStreams()).isEmpty();
    assertThat(project.getZookeepers()).isEmpty();

    Topic topic = project.getTopics().get(0);

    assertThat(topic.getConsumers()).hasSize(1);
    assertThat(topic.getConsumers()).contains(new Consumer("User:App0"));

    assertThat(topic.getProducers()).hasSize(1);
    assertThat(topic.getProducers()).contains(new Producer("User:App1"));

    assertEquals("foo", topic.getName());
  }

  @Test
  public void testWithRBACDescriptor() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor-with-rbac.yaml"));
    Project myProject = topology.getProjects().get(0);

    assertEquals(2, myProject.getRbacRawRoles().size());
    assertEquals(3, myProject.getSchemas().size());
    assertSchemas(
        myProject.getSchemas().get(0),
        "User:App0",
        Collections.singletonList("transactions"),
        RESOURCE_OWNER,
        false);
    assertSchemas(
        myProject.getSchemas().get(1),
        "User:App1",
        Collections.singletonList("contracts"),
        RESOURCE_OWNER,
        false);
    assertSchemas(
        myProject.getSchemas().get(2),
        "User:App2",
        Collections.singletonList("myapp"),
        DEVELOPER_READ,
        true);
    Connector connector = myProject.getConnectors().get(0);
    assertEquals(true, connector.getConnectors().isPresent());
    assertEquals("jdbc-sync", connector.getConnectors().get().get(0));
    assertEquals("ibmmq-source", connector.getConnectors().get().get(1));

    Optional<Map<String, List<User>>> rbacOptional =
        topology.getPlatform().getSchemaRegistry().getRbac();
    assertTrue(rbacOptional.isPresent());

    Set<String> keys = Arrays.asList("Operator").stream().collect(Collectors.toSet());
    assertEquals(keys, rbacOptional.get().keySet());
    assertEquals(2, rbacOptional.get().get("Operator").size());

    Optional<Map<String, List<User>>> kafkaRbacOptional =
        topology.getPlatform().getKafka().getRbac();
    assertTrue(kafkaRbacOptional.isPresent());

    Set<String> kafkaKeys =
        Arrays.asList("SecurityAdmin", "ClusterAdmin").stream().collect(Collectors.toSet());
    assertEquals(kafkaKeys, kafkaRbacOptional.get().keySet());
    assertEquals(1, kafkaRbacOptional.get().get("SecurityAdmin").size());
  }

  @Test
  public void testConnectorArtefactsRetrieval() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor-connector.yaml"));
    Project project = topology.getProjects().get(0);
    List<KafkaConnectArtefact> artefacts = project.getConnectorArtefacts().getConnectors();
    assertEquals(2, artefacts.size());
    assertThat(artefacts.get(0)).hasFieldOrPropertyWithValue("path", "connectors/source-jdbc.json");
    assertThat(artefacts.get(0)).hasFieldOrPropertyWithValue("serverLabel", "connector0");
    assertThat(artefacts.get(1)).hasFieldOrPropertyWithValue("path", "connectors/sink-jdbc.json");
    assertThat(artefacts.get(1)).hasFieldOrPropertyWithValue("serverLabel", "connector0");
  }

  @Test
  public void testBackwardsCompatibleDescriptorForConnectors() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/backwards-comp-descriptor.yaml"));
    Project fooProject = topology.getProjects().get(0);

    assertEquals("foo", fooProject.getName());
    assertEquals(2, fooProject.getConnectors().size());
    assertEquals("User:Connect1", fooProject.getConnectors().get(0).getPrincipal());
    assertEquals("User:Connect2", fooProject.getConnectors().get(1).getPrincipal());
  }

  @Test
  public void testTopicNameWithCustomSeparator() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_SEPARATOR_CONFIG, "_");
    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));

    assertEquals("contextOrg", topology.getContext());

    Project p = topology.getProjects().get(0);

    assertEquals(2, p.getTopics().size());
    assertEquals("contextOrg_source_foo_foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg_source_foo_bar_avro", p.getTopics().get(1).toString());
  }

  @Test
  public void testTopicNameWithCustomPattern() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{source}}.{{context}}.{{project}}.{{topic}}");
    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));

    assertEquals("contextOrg", topology.getContext());

    Project p = topology.getProjects().get(0);

    assertEquals(2, p.getTopics().size());
    assertEquals("source.contextOrg.foo.foo", p.getTopics().get(0).toString());
    assertEquals("source.contextOrg.foo.bar", p.getTopics().get(1).toString());
  }

  @Test
  public void testTopologyWithRepeatedAndMissingDataType() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(
        TOPIC_PREFIX_FORMAT_CONFIG,
        "{{context}}.{{project}}.{{topic}}{% if dataType is defined %}.{{dataType}}{% endif %}");
    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics-datatype.yaml"));

    assertEquals("context", topology.getContext());

    Project p = topology.getProjects().get(0);

    assertEquals(2, p.getTopics().size());
    assertEquals("context.foo.foo", p.getTopics().get(0).toString());
    assertEquals("context.foo.bar.avro", p.getTopics().get(1).toString());
  }

  @Test
  public void testTopicsWithDLQNoFilter() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(4);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.foo.dlq", p.getTopics().get(1).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(2).toString());
    assertEquals("contextOrg.source.foo.bar.avro.dlq", p.getTopics().get(3).toString());
  }

  @Test
  public void testTopicsWithDLQWithDenyList() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_DENY_LIST + ".0", "contextOrg.source.foo.bar.avro");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.foo.dlq", p.getTopics().get(1).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(2).toString());
  }

  @Test
  public void testTopicsWithDLQWithDenyListAndPattern() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_DENY_LIST + ".0", "^.*source.foo.foo$");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(1).toString());
    assertEquals("contextOrg.source.foo.bar.avro.dlq", p.getTopics().get(2).toString());
  }

  @Test
  public void testTopicsWithDLQWithDenyListAndPatternFullDeny() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_DENY_LIST + ".0", "^.*source.foo.*$");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(2);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(1).toString());
  }

  @Test
  public void testTopicsWithDLQWithTopicPattern() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(DLQ_TOPIC_PREFIX_FORMAT_CONFIG, "{{dlq}}.{{context}}.{{project}}");
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_ALLOW_LIST + ".0", "contextOrg.source.foo.bar.avro");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(1).toString());
    assertEquals("dlq.contextOrg.foo", p.getTopics().get(2).toString());
  }

  @Test
  public void testTopicsWithDLQConfigVerify() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_ALLOW_LIST + ".0", "contextOrg.source.foo.bar.avro");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);

    Topic mainTopic = p.getTopics().get(1);
    Topic dlqTopic = p.getTopics().get(2);

    assertEquals("contextOrg.source.foo.bar.avro", mainTopic.toString());
    assertEquals("contextOrg.source.foo.bar.avro.dlq", dlqTopic.toString());
    assertThat(mainTopic.getConfig()).usingRecursiveComparison().isEqualTo(dlqTopic.getConfig());
  }

  @Test
  public void testTopicsWithDLQWithAllowList() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_ALLOW_LIST + ".0", "contextOrg.source.foo.bar.avro");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(1).toString());
    assertEquals("contextOrg.source.foo.bar.avro.dlq", p.getTopics().get(2).toString());
  }

  @Test
  public void testTopicsWithDLQWithAllowAndDenyList() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPOLOGY_DLQ_TOPICS_GENERATE, "true");
    props.put(TOPOLOGY_DQL_TOPICS_ALLOW_LIST + ".0", "contextOrg.source.foo.bar.avro");
    props.put(TOPOLOGY_DQL_TOPICS_DENY_LIST + ".0", "contextOrg.source.bar.bar.avro");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, new PlanMap());

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));
    Project p = topology.getProjects().get(0);

    assertThat(p.getTopics()).hasSize(3);
    assertEquals("contextOrg.source.foo.foo", p.getTopics().get(0).toString());
    assertEquals("contextOrg.source.foo.bar.avro", p.getTopics().get(1).toString());
    assertEquals("contextOrg.source.foo.bar.avro.dlq", p.getTopics().get(2).toString());

    p = topology.getProjects().get(1);

    assertThat(p.getTopics()).hasSize(1);
    assertEquals("contextOrg.source.bar.bar.avro", p.getTopics().get(0).toString());
  }

  @Test(expected = TopologyParsingException.class)
  public void testTopicNameWithUTFCharacters() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics-utf.yaml"));
  }

  @Test
  public void testJsonDescriptorFileSerdes() {
    TopologySerdes parser = new TopologySerdes(new Configuration(), FileType.JSON, new PlanMap());
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.json"));

    assertEquals(1, topology.getProjects().size());
    assertEquals("foo", topology.getProjects().get(0).getName());
    assertEquals(2, topology.getProjects().get(0).getTopics().size());
  }

  @Test
  public void shouldParseSpecialTopics() {
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    var topics = topology.getSpecialTopics();
    assertThat(topics).hasSize(2);
    var topicNames = topics.stream().map(Topic::getName).collect(Collectors.toList());
    assertThat(topicNames).contains("foo");
    assertThat(topicNames).contains("bar");

    Properties props = new Properties();
    props.put(PROJECT_PREFIX_FORMAT_CONFIG, "{{project}}");
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{project}}.{{topic}}");
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config, FileType.YAML, new PlanMap());
    topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    topics = topology.getSpecialTopics();
    assertThat(topics).hasSize(2);

    var topicFullNames = topics.stream().map(Topic::toString).collect(Collectors.toList());
    assertThat(topicFullNames).contains("foo");
    assertThat(topicFullNames).contains("bar");
  }

  private List<Project> buildProjects() {
    Project project = new ProjectImpl("project");
    project.setConsumers(buildConsumers());
    project.setProducers(buildProducers());
    project.setStreams(buildStreams());

    return Collections.singletonList(project);
  }

  private List<KStream> buildStreams() {
    List<KStream> streams = new ArrayList<>();
    HashMap<String, List<String>> topics = new HashMap<>();
    topics.put("read", Arrays.asList("topic1", "topic3"));
    topics.put("write", Arrays.asList("topic2", "topic4"));
    streams.add(new KStream("app3", topics));

    topics = new HashMap<>();
    topics.put("read", Arrays.asList("topic2", "topic4"));
    topics.put("write", Arrays.asList("topic5"));
    streams.add(new KStream("app4", topics));

    return streams;
  }

  private List<Producer> buildProducers() {
    List<Producer> producers = new ArrayList<>();
    producers.add(new Producer("app1"));
    producers.add(new Producer("app3"));
    return producers;
  }

  private List<Consumer> buildConsumers() {
    List<Consumer> consumers = new ArrayList<>();
    consumers.add(new Consumer("app1"));
    consumers.add(new Consumer("app2"));
    return consumers;
  }

  private void assertSchemas(
      Schemas schemas, String principal, List<String> subjects, String role, boolean prefixed) {
    assertEquals("Schemas principal", principal, schemas.getPrincipal());
    assertEquals("Schemas subjects", subjects, schemas.getSubjects());
    assertEquals("Schemas role", role, schemas.getRole());
    assertEquals("Schemas isPrefixed", prefixed, schemas.isPrefixed());
  }
}
