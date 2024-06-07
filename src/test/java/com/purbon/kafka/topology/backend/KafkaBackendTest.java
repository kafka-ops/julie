package com.purbon.kafka.topology.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendConsumer;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendProducer;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.resource.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class KafkaBackendTest {

  @Spy @InjectMocks private KafkaBackend kafkaBackend;

  @Mock private KafkaBackendProducer kafkaBackendProducer;

  @Mock private AtomicBoolean shouldWaitForLoad;

  @Mock private KafkaBackendConsumer kafkaBackendConsumer;

  @Mock private Thread thread;

  private BackendState backendState = new BackendState();

  @Spy private AtomicReference<BackendState> latest = new AtomicReference<>(backendState);

  @Test
  public void testConfigure() {
    Configuration configuration = buildTestConfig();
    when(kafkaBackend.createConsumer(configuration)).thenReturn(kafkaBackendConsumer);
    when(kafkaBackendConsumer.listTopics()).thenReturn(buildTestTopics());
    when(kafkaBackend.createProducer(configuration)).thenReturn(kafkaBackendProducer);
    when(kafkaBackend.createThread()).thenReturn(Mockito.mock(Thread.class));
    ReflectionTestUtils.setField(kafkaBackend, "isCompleted", true);

    kafkaBackend.configure(configuration);

    assertThat(ReflectionTestUtils.getField(kafkaBackend, "instanceId"), is(notNullValue()));
    assertThat(ReflectionTestUtils.getField(kafkaBackend, "latest"), is(notNullValue()));
    verify(kafkaBackend).waitForLoad();
    assertThat(ReflectionTestUtils.getField(kafkaBackend, "consumer"), is(kafkaBackendConsumer));
    assertThat(ReflectionTestUtils.getField(kafkaBackend, "producer"), is(kafkaBackendProducer));
  }

  @NotNull
  private Map<String, List<PartitionInfo>> buildTestTopics() {
    Map<String, List<PartitionInfo>> topics = new HashMap<>();
    topics.put("__julieops_commands", new ArrayList<>());
    return topics;
  }

  @NotNull
  private Configuration buildTestConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("clientConfig", "");
    configMap.put("bootstrap.servers", "localhost:9092");
    configMap.put("client.dns.lookup", "default");
    configMap.put("julie.kafka.config.topic", "julie-kafka-configs");
    Configuration configuration = Configuration.build(configMap);
    return configuration;
  }

  @Test
  public void testSave() throws IOException {
    BackendState backendState = buildBackendState();
    kafkaBackend.save(backendState);
    verify(kafkaBackendProducer).save(backendState);
  }

  @Test
  public void testApply() {
    ReflectionTestUtils.setField(kafkaBackend, "instanceId", "testKey");
    ConsumerRecord<String, BackendState> consumerRecord =
        new ConsumerRecord("testTopic", 1, 100, "testKey", backendState);
    kafkaBackend.apply(consumerRecord);
    AtomicReference<BackendState> resultLatest =
        (AtomicReference<BackendState>) ReflectionTestUtils.getField(kafkaBackend, "latest");
    assertThat(resultLatest.get(), is(backendState));
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
