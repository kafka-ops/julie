package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static com.purbon.kafka.topology.TopicManager.NUM_PARTITIONS;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TopicManagerTest {

  @Mock TopologyBuilderAdminClient adminClient;

  @Mock SchemaRegistryManager schemaRegistryManager;

  BackendController backendController;
  ExecutionPlan plan;

  @Mock PrintStream outputStream;

  private TopicManager topicManager;
  private HashMap<String, String> cliOps;
  private Properties props;
  private Configuration config;

  @BeforeEach
  public void setup() throws IOException {

    Files.deleteIfExists(Paths.get(".cluster-state"));
    backendController = new BackendController();

    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");

    plan = ExecutionPlan.init(backendController, System.out);

    config = new Configuration(cliOps, props);
    topicManager = new TopicManager(adminClient, schemaRegistryManager, config);
  }

  @Test
  void newTopicCreationTest() throws IOException {

    Project project = new ProjectImpl("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
  }

  @Test
  void topicPartitionCountUpdateTest() throws IOException {

    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
    verify(adminClient, times(0)).updatePartitionCount(topicB, topicB.toString());

    ExecutionPlan plan = ExecutionPlan.init(backendController, System.out);
    Configuration config = new Configuration(cliOps, props);
    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    topology = new TopologyImpl();
    project = new ProjectImpl("project");
    topology.addProject(project);

    topicA = new Topic("topicA");
    project.addTopic(topicA);

    topicB = new Topic("topicB", Collections.singletonMap(NUM_PARTITIONS, "12"));
    project.addTopic(topicB);

    doReturn(new Config(Collections.emptyList())).when(adminClient).getActualTopicConfig(any());
    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(0)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(0)).createTopic(topicB, topicB.toString());
    verify(adminClient, times(0)).updatePartitionCount(topicA, topicB.toString());
    verify(adminClient, times(1)).updatePartitionCount(topicB, topicB.toString());
  }

  @Test
  void topicDeleteTest() throws IOException {

    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(ALLOW_DELETE_TOPICS, true);

    Configuration config = new Configuration(cliOps, props);

    plan = ExecutionPlan.init(backendController, System.out);
    topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    // Original Topology
    Topology topology0 = new TopologyImpl();
    Project project0 = new ProjectImpl("project");
    topology0.addProject(project0);

    Topic topicC = new Topic("topicC");
    project0.addTopic(topicC);

    // Topology after delete action
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    Set<String> dummyTopicList = new HashSet<>();
    String topicCFullName = topicC.toString();
    dummyTopicList.add(topicCFullName);
    dummyTopicList.add(
        "_my-internal-topic"); // return an internal topic using the default config values
    when(adminClient.listApplicationTopics()).thenReturn(dummyTopicList);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
    verify(adminClient, times(1)).deleteTopics(Collections.singletonList(topicCFullName));
  }

  @Test
  void topicDeleteWithConfiguredInternalTopicsTest() throws IOException {

    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Properties props = new Properties();
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES, Arrays.asList("foo.", "_"));
    props.put(ALLOW_DELETE_TOPICS, true);

    Configuration config = new Configuration(cliOps, props);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    // Topology after delete action
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    String topicC = "team.project.topicC";
    String topicI1 = "foo.my-internal.topic";
    String topicI2 = "_my-internal.topic";

    Set<String> appTopics = Stream.of(topicC, topicI1, topicI2).collect(Collectors.toSet());
    when(adminClient.listApplicationTopics()).thenReturn(appTopics);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(1)).createTopic(topicB, topicB.toString());
    verify(adminClient, times(1)).deleteTopics(Collections.singletonList(topicC));
  }

  @Test
  void topicDeleteWithConfiguredNoDelete() throws IOException {

    Properties props = new Properties();
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES + ".0", "foo.");
    props.put(KAFKA_INTERNAL_TOPIC_PREFIXES + ".1", "_");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);
    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    // Topology after delete action
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    String topicC = "team.project.topicC";

    Set<String> appTopics = Stream.of(topicC).collect(Collectors.toSet());
    when(adminClient.listApplicationTopics()).thenReturn(appTopics);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(0)).deleteTopics(Collections.singletonList(topicC));
  }

  @Test
  void topicDeleteWithConfiguredNoDeleteOnlyForTopics() throws IOException {

    Properties props = new Properties();
    props.put(ALLOW_DELETE_TOPICS, "true");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    // Topology after delete action
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    String topicC = "team.project.topicC";

    Set<String> appTopics = Collections.singleton(topicC);
    when(adminClient.listApplicationTopics()).thenReturn(appTopics);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).deleteTopics(Collections.singletonList(topicC));
  }

  @Test
  void topicDeleteWithConfiguredNoDeleteOnlyForTopicsAllDisabled() throws IOException {

    Properties props = new Properties();
    props.put(ALLOW_DELETE_TOPICS, "false");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    // Topology after delete action
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB");
    project.addTopic(topicB);

    String topicC = "team.project.topicC";

    Set<String> appTopics = Collections.singleton(topicC);
    when(adminClient.listApplicationTopics()).thenReturn(appTopics);

    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(0)).deleteTopics(Collections.singletonList(topicC));
  }

  @Test
  void dryRunTest() throws IOException {

    plan = ExecutionPlan.init(backendController, outputStream);
    Project project = new ProjectImpl("project");
    Topic topicA = new Topic("topicA");
    project.addTopic(topicA);
    Topic topicB = new Topic("topicB", Collections.singletonMap(NUM_PARTITIONS, "12"));
    project.addTopic(topicB);
    Topology topology = new TopologyImpl();
    topology.addProject(project);

    topicManager.updatePlan(topology, plan);
    plan.run(true);

    verify(outputStream, times(2)).println(any(Action.class));
  }

  @Test
  void toProcessOnlySelectedTopics() throws IOException {

    props.put(TOPIC_MANAGED_PREFIXES, Collections.singletonList("NamespaceA"));
    props.put(TOPIC_PREFIX_FORMAT_CONFIG, "{{topic}}");
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "true");

    Configuration config = new Configuration(cliOps, props);
    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    Project project = new ProjectImpl("project");
    Topic topicA = new Topic("NamespaceA_TopicA", config);
    project.addTopic(topicA);
    Topic topicB = new Topic("NamespaceB_TopicB", config);
    project.addTopic(topicB);
    Topology topology = new TopologyImpl(config);
    topology.addProject(project);

    when(adminClient.listApplicationTopics()).thenReturn(new HashSet<>());
    topicManager.updatePlan(topology, plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, topicA.toString());
    verify(adminClient, times(0)).createTopic(topicB, topicB.toString());
  }

  @Test
  void shouldManageSpecialTopics() throws IOException {
    Topic topicA = new Topic("TopicA");
    topicA.setConsumers(Collections.singletonList(new Consumer("User:foo")));
    topicA.setProducers(Collections.singletonList(new Producer("User:bar")));
    TestTopologyBuilder builder = TestTopologyBuilder.createProject().addSpecialTopic(topicA);

    Configuration config = new Configuration(cliOps, props);
    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    topicManager.updatePlan(builder.buildTopology(), plan);
    plan.run();

    verify(adminClient, times(1)).createTopic(topicA, "TopicA");
  }
}
