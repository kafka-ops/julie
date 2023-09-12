package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
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
import com.purbon.kafka.topology.model.PlanMap;
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

  private static final String TRUSTSTORE_JKS = "/ksql-ssl/truststore/ksqldb.truststore.jks";
  private static final String KEYSTORE_JKS = "/ksql-ssl/keystore/ksqldb.keystore.jks";

  @After
  public void after() {
    connectContainer.stop();
    container.stop();
  }

  @Before
  public void configure() throws IOException {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    connectContainer = new ConnectContainer(container, TRUSTSTORE_JKS, KEYSTORE_JKS);
    connectContainer.start();

    Files.deleteIfExists(Paths.get(".cluster-state"));

    this.plan = ExecutionPlan.init(new BackendController(), System.out);
  }

  @Test
  public void testCreateAndUpdatePathWithLocalClusterState()
      throws IOException, InterruptedException {
    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_CONNECT_ARTEFACTS, "true");
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:" + connectContainer.getHttpsUrl());

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
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:" + connectContainer.getHttpsUrl());

    File file = TestUtils.getResourceFile("/descriptor-connector.yaml");

    testCreateAndUpdatePath(props, file);
  }

  @Test(expected = IOException.class)
  public void shouldDetectChangesInTheRemoteClusterBetweenRuns()
      throws IOException, InterruptedException {
    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_CONNECT_ARTEFACTS, "true");
    props.put(JULIE_VERIFY_STATE_SYNC, true);
    props.put(ALLOW_DELETE_TOPICS, "true");
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:" + connectContainer.getHttpsUrl());

    File file = TestUtils.getResourceFile("/descriptor-connector.yaml");

    testDeleteRemoteButNotLocal(props, file);
  }

  private void testCreateAndUpdatePath(Properties props, File file)
      throws IOException, InterruptedException {

    var topology = initTopology(props, file);

    connectorManager.updatePlan(topology, plan);
    plan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    List<String> connectors = client.list();

    assertThat(connectors).hasSize(2);
    assertThat(connectors).contains("source-jdbc", "sink-jdbc");

    topology.getProjects().get(0).getConnectorArtefacts().getConnectors().remove(0);
    assertThat(topology.getProjects().get(0).getConnectorArtefacts().getConnectors()).hasSize(1);

    ExecutionPlan newPlan = ExecutionPlan.init(new BackendController(), System.out);

    connectorManager.updatePlan(topology, newPlan);
    newPlan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    connectors = client.list();

    assertThat(connectors).hasSize(1);
    assertThat(connectors).contains("sink-jdbc");
  }

  private Topology initTopology(Properties props, File file) throws IOException {
    Configuration config = prepareClientConfig(props);
    parser = new TopologySerdes(config, new PlanMap());
    Topology topology = parser.deserialise(file);
    connectorManager = prepareManager(config, file);
    return topology;
  }

  private Configuration prepareClientConfig(Properties props) {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    props.put(SSL_TRUSTSTORE_LOCATION, TestUtils.getResourceFile(TRUSTSTORE_JKS).getAbsolutePath());
    props.put(SSL_TRUSTSTORE_PASSWORD, "ksqldb");
    props.put(SSL_KEYSTORE_LOCATION, TestUtils.getResourceFile(KEYSTORE_JKS).getAbsolutePath());
    props.put(SSL_KEYSTORE_PASSWORD, "ksqldb");
    props.put(SSL_KEY_PASSWORD, "ksqldb");

    return new Configuration(cliOps, props);
  }

  private KafkaConnectArtefactManager prepareManager(Configuration config, File file)
      throws IOException {
    client = new KConnectApiClient(connectContainer.getHttpsUrl(), config);
    return new KafkaConnectArtefactManager(client, config, file.getAbsolutePath());
  }

  private void testDeleteRemoteButNotLocal(Properties props, File file)
      throws IOException, InterruptedException {

    var topology = initTopology(props, file);

    connectorManager.updatePlan(topology, plan);
    plan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    List<String> connectors = client.list();
    assertThat(connectors).hasSize(2);
    assertThat(connectors).contains("source-jdbc", "sink-jdbc");

    client.delete("source-jdbc");

    ExecutionPlan newPlan = ExecutionPlan.init(new BackendController(), System.out);
    connectorManager.updatePlan(topology, newPlan);
  }

  @Test
  public void testConnectorConfigUpdate() throws IOException, InterruptedException {
    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");
    props.put(ALLOW_DELETE_CONNECT_ARTEFACTS, "true");
    props.put(PLATFORM_SERVERS_CONNECT + ".0", "connector0:" + connectContainer.getHttpsUrl());

    File file = TestUtils.getResourceFile("/descriptor-connector-alone.yaml");

    var topology = initTopology(props, file);

    connectorManager.updatePlan(topology, plan);

    assertThat(plan.getActions()).hasSize(2);

    plan.run();
    // we should wait a bit until the connector starts downstream
    Thread.sleep(1000);

    ExecutionPlan noopPlan = ExecutionPlan.init(new BackendController(), System.out);
    connectorManager.updatePlan(topology, noopPlan);
    assertThat(noopPlan.getActions()).hasSize(0);

    file = TestUtils.getResourceFile("/descriptor-connector-alone-updated.yaml");
    topology = initTopology(props, file);

    ExecutionPlan updatePlan = ExecutionPlan.init(new BackendController(), System.out);
    connectorManager.updatePlan(topology, updatePlan);
    assertThat(updatePlan.getActions()).hasSize(1);

    updatePlan.run();
    // wait for changes to apply
    Thread.sleep(1000);

    ExecutionPlan noopPlan2 = ExecutionPlan.init(new BackendController(), System.out);
    connectorManager.updatePlan(topology, noopPlan2);
    assertThat(noopPlan2.getActions()).hasSize(0);
  }
}
