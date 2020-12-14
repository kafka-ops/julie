package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.backend.RedisBackend.KAFKA_TOPOLOGY_BUILDER_BINDINGS;
import static com.purbon.kafka.topology.backend.RedisBackend.KAFKA_TOPOLOGY_BUILDER_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import redis.clients.jedis.Jedis;

public class RedisBackendTest {

  @Mock Jedis jedis;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private RedisBackend stateProcessor;

  @Before
  public void before() {
    stateProcessor = new RedisBackend(jedis);
  }

  @Test
  public void testSaveBindings() {

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL");

    when(jedis.sadd(eq(KAFKA_TOPOLOGY_BUILDER_BINDINGS), any())).thenReturn(1l);

    stateProcessor.saveBindings(new HashSet<>(Arrays.asList(binding)));

    verify(jedis, times(1)).sadd(eq(KAFKA_TOPOLOGY_BUILDER_BINDINGS), any());
  }

  @Test
  public void testDataLoading() throws IOException {

    when(jedis.get(eq(KAFKA_TOPOLOGY_BUILDER_TYPE))).thenReturn("acls");
    when(jedis.scard(eq(KAFKA_TOPOLOGY_BUILDER_BINDINGS))).thenReturn(10l);
    when(jedis.spop(eq(KAFKA_TOPOLOGY_BUILDER_BINDINGS)))
        .thenReturn("'TOPIC', 'topicA', '*', 'READ', 'User:Connect1', 'LITERAL'")
        .thenReturn("'TOPIC', 'topicB', '*', 'READ', 'User:Connect1', 'LITERAL'");

    Set<TopologyAclBinding> bindings = stateProcessor.loadBindings();

    assertEquals(2, bindings.size());
  }
}
