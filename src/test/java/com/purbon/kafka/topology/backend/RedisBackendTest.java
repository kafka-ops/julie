package com.purbon.kafka.topology.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Collections;
import org.apache.kafka.common.resource.ResourceType;
import org.jetbrains.annotations.NotNull;
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
  private String bucket;

  @Before
  public void before() {
    bucket = "foo";
    stateProcessor = new RedisBackend(jedis, bucket);
  }

  @Test
  public void testSaveBindings() throws IOException {

    BackendState state = buildBackendState();
    stateProcessor.save(state);

    verify(jedis, times(1)).set(eq(bucket), any());
  }

  @Test
  public void testDataLoading() throws IOException {

    BackendState mockedState = buildBackendState();
    when(jedis.get(eq(bucket))).thenReturn(mockedState.asPrettyJson());

    BackendState state = stateProcessor.load();
    assertEquals(1, state.size());
    assertTrue(state.getBindings().iterator().hasNext());
    assertEquals("Topic A", state.getBindings().iterator().next().getResourceName());
  }

  @NotNull
  private BackendState buildBackendState() {
    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic A", "host", "op", "principal", "LITERAL");

    BackendState state = new BackendState();
    state.addBindings(Collections.singleton(binding));
    return state;
  }
}
