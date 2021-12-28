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
import org.apache.kafka.common.TopicPartition;
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

    consumerProperties.put(GROUP_ID_CONFIG, config.getKafkaBackendConsumerGroupId());
    consumer = new KafkaConsumer<>(consumerProperties);

    var topicPartition = new TopicPartition(config.getJulieKafkaConfigTopic(), 0);
    var topicPartitions = Collections.singletonList(topicPartition);
    consumer.assign(topicPartitions);
    consumer.seekToBeginning(topicPartitions);
  }

  public void retrieve(KafkaBackend callback) {
    int times = 0;
    while (running.get()) {
      ConsumerRecords<String, BackendState> records = consumer.poll(Duration.ofSeconds(10));
      callback.complete();
      for (ConsumerRecord<String, BackendState> record : records) {
        callback.apply(record);
      }
      if (records.count() > 0 || times >= config.getKafkaBackendConsumerRetries()) {
        callback.initialLoadFinish();
      }
      times += 1;
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
