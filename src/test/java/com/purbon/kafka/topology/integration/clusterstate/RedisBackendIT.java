package com.purbon.kafka.topology.integration.clusterstate;

import com.purbon.kafka.topology.clusterstate.RedisBackend;
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

public class RedisBackendIT {

  @Rule
  public GenericContainer redis =
      new GenericContainer<>("redis:5.0.3-alpine").withExposedPorts(6379);

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

    Set<TopologyAclBinding> bindings = rsp.load();

    Assert.assertEquals(1, bindings.size());
    Assert.assertEquals(binding.getPrincipal(), bindings.iterator().next().getPrincipal());
  }
}
