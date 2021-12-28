package com.purbon.kafka.topology.integration.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisBackendIT {

  @Rule
  public GenericContainer redis =
      new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

  @Test
  public void testStoreAndFetch() throws IOException {

    String host = redis.getContainerIpAddress();
    int port = redis.getFirstMappedPort();
    RedisBackend rsp = new RedisBackend(host, port);
    rsp.createOrOpen();

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

    Assert.assertEquals(2, recoveredState.getTopics().size());
    assertThat(state.getTopics()).contains("foo", "bar");
    assertThat(state.getConnectors()).hasSize(1);
    assertThat(state.getConnectors()).contains(connector);
    Assert.assertEquals(1, recoveredState.getBindings().size());
    Assert.assertEquals(
        binding.getPrincipal(), recoveredState.getBindings().iterator().next().getPrincipal());
  }
}
