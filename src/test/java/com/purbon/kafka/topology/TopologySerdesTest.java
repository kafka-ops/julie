package com.purbon.kafka.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.ControlCenter;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.SchemaRegistry;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TopologySerdesTest {

  private TopologySerdes parser;

  @Before
  public void setup() {
    parser = new TopologySerdes();
  }

  @Test
  public void testDynamicFirstLevelAttributes() throws IOException, URISyntaxException {

    URL descriptorWithOptionals = getClass().getResource("/descriptor-with-others.yml");

    Topology topology = parser.deserialise(Paths.get(descriptorWithOptionals.toURI()).toFile());
    assertEquals("team.source.foo.bar.zet", topology.buildNamePrefix());

    URL descriptorWithoutOptionals = getClass().getResource("/descriptor.yaml");

    Topology anotherTopology =
        parser.deserialise(Paths.get(descriptorWithoutOptionals.toURI()).toFile());
    assertEquals("team.source", anotherTopology.buildNamePrefix());
  }

  @Test
  public void testTopologySerialisation() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setTeam("team");
    topology.setProjects(buildProjects());

    String topologyYamlString = parser.serialise(topology);
    Topology deserTopology = parser.deserialise(topologyYamlString);

    assertEquals(topology.getTeam(), deserTopology.getTeam());
    assertEquals(topology.getProjects().size(), deserTopology.getProjects().size());
  }

  @Test
  public void testTopicConfigSerdes() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setTeam("team");

    Topic topic = new TopicImpl();
    topic.setName("foo");
    HashMap<String, String> topicConfig = new HashMap<>();
    topicConfig.put("num.partitions", "1");
    topicConfig.put("replication.factor", "1");
    topic.setConfig(topicConfig);

    Topic topicBar = new TopicImpl("bar", "avro");
    HashMap<String, String> topicBarConfig = new HashMap<>();
    topicBarConfig.put("num.partitions", "1");
    topicBarConfig.put("replication.factor", "1");
    topicBar.setConfig(topicBarConfig);

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
    assertEquals(
        topic.getConfig().get("num.partitions"), serdesTopic.getConfig().get("num.partitions"));
  }

  @Test
  public void testTopicWithDataType() throws IOException {

    Project project = new ProjectImpl("foo");

    Topology topology = new TopologyImpl();
    topology.setTeam("team");

    project.setTopologyPrefix(topology.buildNamePrefix());
    topology.addProject(project);

    Topic topic = new TopicImpl("foo", "json");
    HashMap<String, String> topicConfig = new HashMap<>();
    topicConfig.put("num.partitions", "3");
    topicConfig.put("replication.factor", "2");
    topic.setConfig(topicConfig);

    project.addTopic(topic);

    Topic topic2 = new TopicImpl("topic2");
    topic.setConfig(topicConfig);
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

  @Test(expected = IOException.class)
  public void testTopologyWithNoTeam() throws IOException, URISyntaxException {
    URL topologyDescriptor = getClass().getResource("/descriptor-with-no-team.yaml");
    parser.deserialise(Paths.get(topologyDescriptor.toURI()).toFile());
  }

  @Test(expected = IOException.class)
  public void testTopologyWithNoProject() throws IOException, URISyntaxException {
    URL topologyDescriptor = getClass().getResource("/descriptor-with-no-project.yaml");
    parser.deserialise(Paths.get(topologyDescriptor.toURI()).toFile());
  }

  @Test
  public void testPlaformProcessing() throws IOException, URISyntaxException {

    URL topologyDescriptor = getClass().getResource("/descriptor.yaml");

    Topology topology = parser.deserialise(Paths.get(topologyDescriptor.toURI()).toFile());

    List<SchemaRegistry> listOfSR = topology.getPlatform().getSchemaRegistry();
    assertEquals(2, listOfSR.size());
    assertEquals("User:SchemaRegistry01", listOfSR.get(0).getPrincipal());
    assertEquals("foo", listOfSR.get(0).topicString());
    assertEquals("bar", listOfSR.get(0).groupString());
    assertEquals("User:SchemaRegistry02", listOfSR.get(1).getPrincipal());

    List<ControlCenter> listOfC3 = topology.getPlatform().getControlCenter();

    assertEquals(1, listOfC3.size());
    assertEquals("User:ControlCenter", listOfC3.get(0).getPrincipal());
    assertEquals("controlcenter", listOfC3.get(0).getAppId());
  }

  @Test
  public void testOnlyTopics() throws URISyntaxException, IOException {

    URL topologyDescriptor = getClass().getResource("/descriptor-only-topics.yaml");
    Topology topology = parser.deserialise(Paths.get(topologyDescriptor.toURI()).toFile());

    assertTrue(topology.getProjects().get(0).getConnectors().isEmpty());
    assertTrue(topology.getProjects().get(0).getProducers().isEmpty());
    assertTrue(topology.getProjects().get(0).getStreams().isEmpty());
    assertTrue(topology.getProjects().get(0).getZookeepers().isEmpty());
  }

  @Test
  public void testWithRBACDescriptor() throws IOException, URISyntaxException {
    URL descriptor = getClass().getResource("/descriptor-with-rbac.yaml");

    Topology topology = parser.deserialise(Paths.get(descriptor.toURI()).toFile());
    Project myProject = topology.getProjects().get(0);

    assertEquals(2, myProject.getSchemas().size());
    assertEquals("User:App0", myProject.getSchemas().get(0).getPrincipal());
    assertEquals(1, myProject.getSchemas().get(0).getSubjects().size());
  }

  private List<Project> buildProjects() {

    Project project = new ProjectImpl();
    project.setName("project");
    project.setConsumers(buildConsumers());
    project.setProducers(buildProducers());
    project.setStreams(buildStreams());

    return Collections.singletonList(project);
  }

  private List<KStream> buildStreams() {
    List<KStream> streams = new ArrayList<KStream>();
    HashMap<String, List<String>> topics = new HashMap<String, List<String>>();
    topics.put("read", Arrays.asList("topic1", "topic3"));
    topics.put("write", Arrays.asList("topic2", "topic4"));
    streams.add(new KStream("app3", topics));

    topics = new HashMap<String, List<String>>();
    topics.put("read", Arrays.asList("topic2", "topic4"));
    topics.put("write", Arrays.asList("topic5"));
    streams.add(new KStream("app4", topics));

    return streams;
  }

  private List<Producer> buildProducers() {
    List<Producer> producers = new ArrayList<Producer>();
    producers.add(new Producer("app1"));
    producers.add(new Producer("app3"));
    return producers;
  }

  private List<Consumer> buildConsumers() {
    List<Consumer> consumers = new ArrayList<Consumer>();
    consumers.add(new Consumer("app1"));
    consumers.add(new Consumer("app2"));
    return consumers;
  }
}
