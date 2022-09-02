package com.purbon.kafka.topology.integration.backend;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.JULIE_INSTANCE_ID;
import static com.purbon.kafka.topology.Constants.JULIE_KAFKA_CONSUMER_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.Constants;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.KafkaBackend;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaBackendIT {

  Configuration config;
  Properties props;

  private static SaslPlaintextKafkaContainer container;

  @BeforeEach
  void before() throws IOException {

    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();

    props = new Properties();
    props.put(JULIE_INSTANCE_ID, "1234");

    Map<String, Object> saslConfig =
        ContainerTestUtils.getSaslConfig(
            container.getBootstrapServers(),
            SaslPlaintextKafkaContainer.DEFAULT_SUPER_USERNAME,
            SaslPlaintextKafkaContainer.DEFAULT_SUPER_PASSWORD);
    saslConfig.forEach((k, v) -> props.setProperty(k, String.valueOf(v)));

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, container.getBootstrapServers());

    config = new Configuration(cliOps, props);

    var adminClient = ContainerTestUtils.getSaslAdminClient(container);
    var topologyAdminClient = new TopologyBuilderAdminClient(adminClient);
    topologyAdminClient.createTopic(config.getJulieKafkaConfigTopic());
  }

  @AfterEach
  void after() {
    container.stop();
  }

  @Test
  void expectedFlow() throws IOException, InterruptedException {

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");

    BackendState state = new BackendState();
    state.addBindings(Collections.singleton(binding));

    KafkaBackend backend = new KafkaBackend();
    backend.configure(config);

    backend.save(state);
    backend.close();

    KafkaBackend newBackend = new KafkaBackend();

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, container.getBootstrapServers());
    props.put(JULIE_KAFKA_CONSUMER_GROUP_ID, "julieops" + System.currentTimeMillis());
    Configuration config = new Configuration(cliOps, props);
    newBackend.configure(config);

    Thread.sleep(3000);

    BackendState newState = newBackend.load();
    assertThat(newState.size()).isEqualTo(1);
    assertThat(newState.getBindings()).contains(binding);
    newBackend.close();
  }

  @Test
  void wrongConfig() {
    assertThrows(
        IOException.class,
        () -> {
          KafkaBackend backend = new KafkaBackend();

          HashMap<String, String> cliOps = new HashMap<>();
          cliOps.put(BROKERS_OPTION, container.getBootstrapServers());

          props.put(Constants.JULIE_KAFKA_CONFIG_TOPIC, "foo");

          Configuration config = new Configuration(cliOps, props);

          backend.configure(config);
          backend.close();
        });
  }
}
