package com.purbon.kafka.topology.backend.kafka;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.KafkaBackend;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.Serdes;

public class KafkaBackendConsumer {

  private Configuration config;
  private KafkaConsumer<String, BackendState> consumer;

  private AtomicBoolean running;

  public KafkaBackendConsumer(Configuration config) {
    this.config = config;
    this.running = new AtomicBoolean(false);
  }

  public void configure() {
    Properties consumerProperties = config.asProperties();
    consumerProperties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Serdes.String().deserializer().getClass());
    var serde = new JsonDeserializer<>(BackendState.class);
    consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, serde.getClass());
    consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    var groupId = consumerProperties.getProperty(GROUP_ID_CONFIG, config.getJulieInstanceId());
    consumerProperties.put(GROUP_ID_CONFIG, groupId);
    consumer = new KafkaConsumer<>(consumerProperties);
    consumer.subscribe(Collections.singletonList(config.getJulieKafkaConfigTopic()));
  }

  public void retrieve(KafkaBackend callback) {

    while (running.get()) {
      ConsumerRecords<String, BackendState> records = consumer.poll(Duration.ofSeconds(1));
      callback.complete();
      for (ConsumerRecord<String, BackendState> record : records) {
        callback.apply(record);
      }
      consumer.commitAsync();
    }
  }

  public void stop() {
    running.set(false);
    consumer.wakeup();
  }

  public void start() {
    running.set(true);
  }

  public Map<String, List<PartitionInfo>> listTopics() {
    return consumer.listTopics();
  }
}
