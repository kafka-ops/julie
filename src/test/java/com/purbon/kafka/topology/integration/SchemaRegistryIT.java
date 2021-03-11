package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.CommandLineInterface.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Configuration.TOPOLOGY_TOPIC_STATE_FROM_CLUSTER;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.integration.containerutils.SchemaRegistryContainer;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SchemaRegistryIT {

  static SaslPlaintextKafkaContainer container;
  static SchemaRegistryContainer schemaRegistryContainer;
  private TopologySerdes parser;
  private Configuration config;
  private ExecutionPlan plan;

  @BeforeClass
  public static void setup() {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    schemaRegistryContainer = new SchemaRegistryContainer(container);
    schemaRegistryContainer.start();
  }

  @Before
  public void configure() throws IOException {
    parser = new TopologySerdes();

    Properties props = new Properties();
    props.put(TOPOLOGY_TOPIC_STATE_FROM_CLUSTER, "false");

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(ALLOW_DELETE_OPTION, "true");

    config = new Configuration(cliOps, props);

    this.plan = ExecutionPlan.init(new BackendController(), System.out);
  }

  @Test
  public void testSetup() throws IOException {
    AdminClient kafkaAdminClient = ContainerTestUtils.getSaslAdminClient(container);
    TopologyBuilderAdminClient adminClient = new TopologyBuilderAdminClient(kafkaAdminClient);

    File file = TestUtils.getResourceFile("/descriptor-schemas.yaml");

    String schemaRegistryUrl =
        schemaRegistryContainer.getHost() + ":" + schemaRegistryContainer.getExposedPorts().get(0);
    RestService restService = new RestService(schemaRegistryUrl);

    List<SchemaProvider> providers =
        Arrays.asList(
            new AvroSchemaProvider(), new JsonSchemaProvider(), new ProtobufSchemaProvider());
    SchemaRegistryClient schemaRegistryClient =
        new CachedSchemaRegistryClient(restService, 10, providers, null, null);

    SchemaRegistryManager schemaRegistryManager =
        new SchemaRegistryManager(schemaRegistryClient, file.getAbsolutePath());

    TopicManager topicManager = new TopicManager(adminClient, schemaRegistryManager, config);

    topicManager.apply(parser.deserialise(file), plan);
    plan.run();
  }

  @AfterClass
  public static void after() {
    schemaRegistryContainer.stop();
    container.stop();
  }
}
