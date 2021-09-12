package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.integration.containerutils.ConnectContainer;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectApiClientIT {

  static SaslPlaintextKafkaContainer container;
  static ConnectContainer connectContainer;

  KConnectApiClient client;

  private static final String TRUSTSTORE_JKS = "/ksql-ssl/truststore/ksqldb.truststore.jks";
  private static final String KEYSTORE_JKS = "/ksql-ssl/keystore/ksqldb.keystore.jks";

  @BeforeClass
  public static void setup() {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    connectContainer = new ConnectContainer(container, TRUSTSTORE_JKS, KEYSTORE_JKS);
    connectContainer.start();
  }

  @AfterClass
  public static void after() {
    connectContainer.stop();
    container.stop();
  }

  @Before
  public void configure() {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Configuration config = new Configuration(cliOps, new Properties());
    client = new KConnectApiClient(connectContainer.getHttpUrl(), config);
  }

  @Test
  public void testAddRetrieveAndDeleteConnector() throws IOException {
    String connectorName = "file-source-connector";
    String connectorConfig =
        "{\n"
            + "    \"name\": \""
            + connectorName
            + "\",\n"
            + "    \"config\": {\n"
            + "        \"connector.class\": \"FileStreamSource\",\n"
            + "        \"tasks.max\": \"1\",\n"
            + "        \"file\": \"/tmp/test.txt\",\n"
            + "        \"topic\": \"connect-test\"\n"
            + "    }\n"
            + "}";

    client.add(connectorConfig);

    List<String> connectors = client.list();
    assertThat(connectors).contains(connectorName);

    client.delete(connectorName);

    connectors = client.list();
    assertThat(connectors).isEmpty();
  }

  @Test
  public void testAddStartStopConnector() throws IOException, InterruptedException {
    String connectorName = "file-source-connector";
    String connectorConfig =
        "{\n"
            + "    \"name\": \""
            + connectorName
            + "\",\n"
            + "    \"config\": {\n"
            + "        \"connector.class\": \"FileStreamSource\",\n"
            + "        \"tasks.max\": \"1\",\n"
            + "        \"file\": \"/tmp/test.txt\",\n"
            + "        \"topic\": \"connect-test\"\n"
            + "    }\n"
            + "}";

    client.add(connectorConfig);
    Thread.sleep(1000);

    String status = client.status(connectorName);
    assertThat(status).isEqualTo("RUNNING");

    client.pause(connectorName);
    Thread.sleep(1000);

    status = client.status(connectorName);
    assertThat(status).isEqualTo("PAUSED");
  }
}
