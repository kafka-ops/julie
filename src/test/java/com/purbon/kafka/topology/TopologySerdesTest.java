package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPIC_PREFIX_FORMAT_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.TOPIC_PREFIX_SEPARATOR_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.platform.ControlCenterInstance;
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
    parser = new TopologySerdes();
  }

  @Test
  public void testDynamicFirstLevelAttributes() {
    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-with-others.yml"));
    Project project = topology.getProjects().get(0);
    assertThat(project.namePrefix()).startsWith("contextOrg.source.foo.bar.zet");

    Topology anotherTopology = parser.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    Project anotherProject = anotherTopology.getProjects().get(0);

    assertEquals("contextOrg.source.foo", anotherProject.namePrefix());
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
    Topic topic = new TopicImpl("foo", topicConfig);

    HashMap<String, String> topicBarConfig = new HashMap<>();
    topicBarConfig.put("num.partitions", "1");
    topicBarConfig.put("replication.factor", "1");
    Topic topicBar = new TopicImpl("bar", "avro", topicBarConfig);

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
    Topic topic = new TopicImpl("foo", "json", topicConfig);
    project.addTopic(topic);

    Topic topic2 = new TopicImpl("topic2", topicConfig);
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

  @Test(expected = TopologyParsingException.class)
  public void testTopologyWithNoProject() {
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
  public void testSchemaSerdes() {
    parser.deserialise(TestUtils.getResourceFile("/descriptor-wrong-schemas.yaml"));
  }

  @Test
  public void testPlaformProcessing() {
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
    assertEquals(2, myProject.getSchemas().size());
    assertEquals("User:App0", myProject.getSchemas().get(0).getPrincipal());
    assertEquals(1, myProject.getSchemas().get(0).getSubjects().size());

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
  public void testTopicNameWithCustomSeparator() {
    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_SEPARATOR_CONFIG, "_");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config);

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
    cliOps.put(ADMIN_CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{source}}.{{context}}.{{project}}.{{topic}}");
    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    TopologySerdes parser = new TopologySerdes(config);

    Topology topology =
        parser.deserialise(TestUtils.getResourceFile("/descriptor-only-topics.yaml"));

    assertEquals("contextOrg", topology.getContext());

    Project p = topology.getProjects().get(0);

    assertEquals(2, p.getTopics().size());
    assertEquals("source.contextOrg.foo.foo", p.getTopics().get(0).toString());
    assertEquals("source.contextOrg.foo.bar", p.getTopics().get(1).toString());
  }

  @Test
  public void testJsonDescriptorFileSerdes() {
    TopologySerdes parser = new TopologySerdes(new TopologyBuilderConfig(), FileType.JSON);
    Topology topology = parser.deserialise(TestUtils.getResourceFile("/descriptor.json"));

    assertEquals(1, topology.getProjects().size());
    assertEquals("foo", topology.getProjects().get(0).getName());
    assertEquals(2, topology.getProjects().get(0).getTopics().size());
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
}
