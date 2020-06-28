package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static org.junit.Assert.assertEquals;

import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class KafkaTopologyBuilderTest {

  @Mock KafkaAdminClient kafkaAdminClient;

  @Mock TopologyBuilderAdminClient topologyAdminClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  Map<String, String> cliOps;
  Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  public void buildTopicNameTest() throws URISyntaxException, IOException {

    URL dirOfDescriptors = getClass().getResource("/dir");
    String fileOrDirPath = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    KafkaTopologyBuilder builder =
        new KafkaTopologyBuilder(
            fileOrDirPath, cliOps, new TopologySerdes(), props, topologyAdminClient, false);

    Topology topology = builder.buildTopology(fileOrDirPath);

    assertEquals(4, topology.getProjects().size());
  }

  @Test
  public void testRbacSetup() throws URISyntaxException, IOException {
    URL dirOfDescriptors = getClass().getResource("/dir");
    String fileOrDirPath = Paths.get(dirOfDescriptors.toURI()).toFile().toString();

    props.put(ACCESS_CONTROL_IMPLEMENTATION_CLASS, "com.purbon.kafka.topology.roles.RBACProvider");
    props.put(MDS_SERVER, "http://localhost:8090");
    props.put(MDS_USER_CONFIG, "alice");
    props.put(MDS_PASSWORD_CONFIG, "alice-secret");
    props.put(MDS_KAFKA_CLUSTER_ID_CONFIG, "UtBZ3rTSRtypmmkAL1HbHw");

    KafkaTopologyBuilder builder =
        new KafkaTopologyBuilder(
            fileOrDirPath, cliOps, new TopologySerdes(), props, topologyAdminClient, false);

    builder.buildAccessControlProvider();
  }
}
