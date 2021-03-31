package com.purbon.kafka.topology.integration.backend;

import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
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

    rsp.saveType("acls");
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");

    BackendState state = new BackendState();
    state.addBindings(Collections.singleton(binding));
    rsp.save(state);

    BackendState recoveredState = rsp.load();

    Assert.assertEquals(1, recoveredState.getBindings().size());
    Assert.assertEquals(
        binding.getPrincipal(), recoveredState.getBindings().iterator().next().getPrincipal());
  }
}
