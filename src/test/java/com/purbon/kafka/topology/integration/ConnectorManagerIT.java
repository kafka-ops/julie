package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.KafkaConnectArtefactManager;
import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.integration.containerutils.ConnectContainer;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectorManagerIT {

  static SaslPlaintextKafkaContainer container;
  static ConnectContainer connectContainer;

  private KConnectApiClient client;
  private KafkaConnectArtefactManager connectorManager;
  private TopologySerdes parser;
  private ExecutionPlan plan;

  @After
  public void after() {
    connectContainer.stop();
    container.stop();
  }

  @Before
  public void configure() throws IOException {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    connectContainer = new ConnectContainer(container);
    connectContainer.start();

    Files.deleteIfExists(Paths.get(".cluster-state"));

    client = new KConnectApiClient(connectContainer.getUrl());
    parser = new TopologySerdes();

    this.plan = ExecutionPlan.init(new BackendController(), System.out);
  }

  @Test
  public void testCreateAndUpdatePathWithLocalClusterState()
      throws IOException, InterruptedException {
    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_CONNECT_ARTEFACTS, "true");

    File file = TestUtils.getResourceFile("/descriptor-connector.yaml");

    testCreateAndUpdatePath(props, file);
  }

  @Test
  public void testCreateAndUpdatePathWithRemoveClusterState()
      throws IOException, InterruptedException {
    Properties props = new Properties();
    props.put(TOPOLOGY_STATE_FROM_CLUSTER, "true");
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_CONNECT_ARTEFACTS, "true");
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:" + client.getServer());

    File file = TestUtils.getResourceFile("/descriptor-connector.yaml");

    testCreateAndUpdatePath(props, file);
  }

  private void testCreateAndUpdatePath(Properties props, File file)
      throws IOException, InterruptedException {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    Configuration config = new Configuration(cliOps, props);

    Topology topology = parser.deserialise(file);
    connectorManager = new KafkaConnectArtefactManager(client, config, file.getAbsolutePath());

    connectorManager.updatePlan(plan, topology);
    plan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    List<String> connectors = client.list();

    assertThat(connectors).hasSize(2);
    assertThat(connectors).contains("source-jdbc", "sink-jdbc");

    topology.getProjects().get(0).getConnectorArtefacts().getConnectors().remove(0);
    assertThat(topology.getProjects().get(0).getConnectorArtefacts().getConnectors()).hasSize(1);

    ExecutionPlan newPlan = ExecutionPlan.init(new BackendController(), System.out);

    connectorManager.updatePlan(newPlan, topology);
    newPlan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    connectors = client.list();

    assertThat(connectors).hasSize(1);
    assertThat(connectors).contains("sink-jdbc");
  }
}
