package com.purbon.kafka.topology.integration.backend;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.ALLOW_DELETE_TOPICS;
import static com.purbon.kafka.topology.Constants.REDIS_HOST_CONFIG;
import static com.purbon.kafka.topology.Constants.REDIS_PORT_CONFIG;
import static com.purbon.kafka.topology.Constants.STATE_PROCESSOR_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.Constants.TOPOLOGY_TOPIC_STATE_FROM_CLUSTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

@ExtendWith(MockitoExtension.class)
@Testcontainers
public class RedisBackendIT {

  @Container
  public GenericContainer redis =
      new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

  private static SaslPlaintextKafkaContainer container;
  private TopicManager topicManager;
  private AdminClient kafkaAdminClient;

  private ExecutionPlan plan;
  private Jedis jedis;
  private static String bucket;

  @BeforeAll
  public static void setup() {
    bucket = "bucket";
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
  }

  @AfterAll
  public static void teardown() {
    container.stop();
  }

  @BeforeEach
  void before() throws IOException {
    Files.deleteIfExists(Paths.get(".cluster-state"));

    kafkaAdminClient = ContainerTestUtils.getSaslAdminClient(container);
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);

    final SchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
    final SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, System.getProperty("user.dir"));

    this.jedis = new Jedis(redis.getContainerIpAddress(), redis.getFirstMappedPort());
    var backend = new RedisBackend(jedis, bucket);

    this.plan = ExecutionPlan.init(new BackendController(backend), System.out);

    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, true);
    props.put(ALLOW_DELETE_TOPICS, true);
    props.put(
        STATE_PROCESSOR_IMPLEMENTATION_CLASS, "com.purbon.kafka.topology.backend.RedisBackend");
    props.put(REDIS_HOST_CONFIG, redis.getContainerIpAddress());
    props.put(REDIS_PORT_CONFIG, redis.getFirstMappedPort());

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    this.topicManager = new TopicManager(adminClient, schemaRegistryManager, config);
  }

  @Test
  void storeAndFetch() throws IOException {

    String host = redis.getContainerIpAddress();
    int port = redis.getFirstMappedPort();
    RedisBackend rsp = new RedisBackend(host, port, bucket);
    rsp.load();

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");

    List<String> topics = Arrays.asList("foo", "bar");
    var connector = new KafkaConnectArtefact("path", "label", "name");
    List<KafkaConnectArtefact> connectors = Arrays.asList(connector);
    BackendState state = new BackendState();
    state.addTopics(topics);
    state.addBindings(Collections.singleton(binding));
    state.addConnectors(connectors);

    rsp.save(state);

    BackendState recoveredState = rsp.load();

    assertEquals(2, recoveredState.getTopics().size());
    assertThat(state.getTopics()).contains("foo", "bar");
    assertThat(state.getConnectors()).hasSize(1);
    assertThat(state.getConnectors()).contains(connector);
    assertEquals(1, recoveredState.getBindings().size());
    assertEquals(
        binding.getPrincipal(), recoveredState.getBindings().iterator().next().getPrincipal());
  }

  @Test
  void topicCreation() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("testTopicCreation");
    Project project = new ProjectImpl("project");
    topology.addProject(project);

    HashMap<String, String> config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topic topicA = new Topic("topicA", config);
    project.addTopic(topicA);

    config = new HashMap<>();
    config.put(TopicManager.NUM_PARTITIONS, "1");
    config.put(TopicManager.REPLICATION_FACTOR, "1");

    Topic topicB = new Topic("topicB", config);
    project.addTopic(topicB);

    topicManager.updatePlan(topology, plan);
    plan.run();

    String content = jedis.get(bucket);
    assertThat(content)
        .contains(
            "\"topics\" : [ \"testTopicCreation.project.topicB\", \"testTopicCreation.project.topicA\" ]");
  }
}
