package com.purbon.kafka.topology.integration.backend;

import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    rsp.saveBindings(new HashSet<>(Arrays.asList(binding)));

    Set<TopologyAclBinding> bindings = rsp.loadBindings();

    Assert.assertEquals(1, bindings.size());
    Assert.assertEquals(binding.getPrincipal(), bindings.iterator().next().getPrincipal());
  }
}
