package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.backend.RedisBackend.JULIE_OPS_BINDINGS;
import static com.purbon.kafka.topology.backend.RedisBackend.JULIE_OPS_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
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

    when(jedis.sadd(eq(JULIE_OPS_BINDINGS), any())).thenReturn(1L);

    BackendState state = new BackendState();
    state.addBindings(Collections.singleton(binding));
    stateProcessor.save(state);

    verify(jedis, times(1)).sadd(eq(JULIE_OPS_BINDINGS), any());
  }

  @Test
  public void testDataLoading() throws IOException {

    when(jedis.get(eq(JULIE_OPS_TYPE))).thenReturn("acls");
    when(jedis.scard(eq(JULIE_OPS_BINDINGS))).thenReturn(10L);
    when(jedis.spop(eq(JULIE_OPS_BINDINGS)))
        .thenReturn(
            "'TOPIC', 'topicA', '*', 'READ', 'User:C=NO,CN=John Doe,emailAddress=john.doe@example.com', 'LITERAL'")
        .thenReturn("'TOPIC', 'topicB', '*', 'READ', 'User:Connect1', 'LITERAL'");

    BackendState state = stateProcessor.load();
    assertEquals(2, state.size());
  }
}
