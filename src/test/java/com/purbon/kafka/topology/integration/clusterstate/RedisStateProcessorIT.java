package com.purbon.kafka.topology.integration.clusterstate;

import com.purbon.kafka.topology.clusterstate.RedisSateProcessor;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class RedisStateProcessorIT {

  @Rule
  public GenericContainer redis =
      new GenericContainer<>("redis:5.0.3-alpine").withExposedPorts(6379);

  @Test
  public void testStoreAndFetch() throws IOException {

    String host = redis.getContainerIpAddress();
    int port = redis.getFirstMappedPort();
    RedisSateProcessor rsp = new RedisSateProcessor(host, port);
    rsp.createOrOpen();

    rsp.saveType("acls");
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");
    rsp.saveBindings(Arrays.asList(binding));

    List<TopologyAclBinding> bindings = rsp.load();

    Assert.assertEquals(1, bindings.size());
    Assert.assertEquals(binding.getPrincipal(), bindings.get(0).getPrincipal());
  }
}
