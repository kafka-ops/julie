package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.KSqlArtefactManager;
import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.api.ksql.KsqlClientConfig;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.KsqlContainer;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KsqlManagerIT {

  static SaslPlaintextKafkaContainer container;
  static KsqlContainer ksqlContainer;
  private TopologySerdes parser;
  private KsqlApiClient client;
  private ExecutionPlan plan;

  @AfterEach
  public void after() {
    ksqlContainer.stop();
    container.stop();
  }

  @BeforeEach
  public void configure() throws IOException {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    ksqlContainer = new KsqlContainer(container);
    ksqlContainer.start();

    Files.deleteIfExists(Paths.get(".cluster-state"));

    KsqlClientConfig ksqlClientConfig =
        KsqlClientConfig.builder().setServer(ksqlContainer.getUrl()).build();
    client = new KsqlApiClient(ksqlClientConfig);
    parser = new TopologySerdes();

    this.plan = ExecutionPlan.init(new BackendController(), System.out);
  }

  @Test
  void createAndUpdatePathWithLocalClusterState() throws IOException {
    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_KSQL_ARTEFACTS, "true");

    File file = TestUtils.getResourceFile("/descriptor-ksql.yaml");

    testCreateAndUpdatePath(props, file);
  }

  @Test
  void createAndUpdatePathWithRemoveClusterState() throws IOException {
    Properties props = new Properties();
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, "true");
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_KSQL_ARTEFACTS, "true");
    props.put(PLATFORM_SERVER_KSQL_URL, "http://" + client.getServer());

    File file = TestUtils.getResourceFile("/descriptor-ksql.yaml");

    testCreateAndUpdatePath(props, file);
  }

  @Test
  void shouldDetectChangesInTheRemoteClusterBetweenRuns() throws IOException {
    assertThrows(
        IOException.class,
        () -> {
          Properties props = new Properties();
          props.put(TOPOLOGY_STATE_FROM_CLUSTER, "false");
          props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
          props.put(ALLOW_DELETE_KSQL_ARTEFACTS, "true");
          props.put(JULIE_VERIFY_STATE_SYNC, true);
          props.put(PLATFORM_SERVER_KSQL_URL, "http://" + client.getServer());

          File file = TestUtils.getResourceFile("/descriptor-ksql.yaml");

          testDeleteRemoteButNotLocal(props, file);
        });
  }

  public void testCreateAndUpdatePath(Properties props, File file) throws IOException {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    Topology topology = parser.deserialise(file);

    KSqlArtefactManager kam = new KSqlArtefactManager(client, config, file.getAbsolutePath());

    kam.updatePlan(topology, plan);
    plan.run();

    List<String> streams = client.listStreams();
    assertThat(streams).hasSize(1);
    assertThat(streams.get(0)).contains("{\"path\":\"\",\"name\":\"RIDERLOCATIONS\"");

    topology.getProjects().get(0).getKsqlArtefacts().getStreams().remove(0);

    assertThat(topology.getProjects().get(0).getKsqlArtefacts().getStreams()).hasSize(0);

    ExecutionPlan newPlan = ExecutionPlan.init(new BackendController(), System.out);

    kam.updatePlan(topology, newPlan);
    newPlan.run();

    streams = client.listStreams();
    assertThat(streams).hasSize(0);
  }

  public void testDeleteRemoteButNotLocal(Properties props, File file) throws IOException {

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    Topology topology = parser.deserialise(file);

    KSqlArtefactManager kam = new KSqlArtefactManager(client, config, file.getAbsolutePath());

    kam.updatePlan(topology, plan);
    plan.run();

    var first = plan.getKSqlArtefacts().stream().findFirst().get();
    client.delete(first.getName(), "TABLE");

    ExecutionPlan newPlan = ExecutionPlan.init(new BackendController(), System.out);
    kam.updatePlan(topology, newPlan);
  }
}
