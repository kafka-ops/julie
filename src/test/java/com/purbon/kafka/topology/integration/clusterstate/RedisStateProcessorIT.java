package com.purbon.kafka.topology.integration.clusterstate;

import com.purbon.kafka.topology.TopicManager;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.clusterstate.RedisSateProcessor;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

public class RedisStateProcessorIT {


  @Rule
  public GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine")
      .withExposedPorts(6379);

  @Test
  public void testStoreAndFetch() throws IOException {

    String host = redis.getContainerIpAddress();
    int port = redis.getFirstMappedPort();
    RedisSateProcessor rsp = new RedisSateProcessor(host, port);
    rsp.createOrOpen();

    rsp.saveType("acls");
    TopologyAclBinding binding = TopologyAclBinding.build(ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");
    rsp.saveBindings(Arrays.asList(binding));

    List<TopologyAclBinding> bindings = rsp.load();

    Assert.assertEquals(1, bindings.size());
    Assert.assertEquals(binding.getPrincipal(), bindings.get(0).getPrincipal());
  }

}
