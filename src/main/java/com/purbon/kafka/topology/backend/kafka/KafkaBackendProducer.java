package com.purbon.kafka.topology.backend.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.utils.JSON;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaBackendProducer {

  private String instanceId;
  private Configuration config;
  private KafkaProducer<String, byte[]> producer;
  private ByteArrayChunker chunker;

  public KafkaBackendProducer(Configuration config, ByteArrayChunker chunker) {
    this.config = config;
    this.instanceId = config.getJulieInstanceId();
    this.chunker = chunker;
  }

  public void configure() {
    if (config.isDryRun()) {
      return;
    }
    Properties props = config.asProperties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    producer = new KafkaProducer<>(props);
  }

  public void save(BackendState backendState) {
    if (config.isDryRun()) {
      return;
    }
    List<byte[]> chunks = chunkify(backendState);
    int key = 1;
    for (byte[] chunk : chunks) {
      String keyString = String.format("%s-%06d", instanceId, key);
      ProducerRecord<String, byte[]> record =
          new ProducerRecord<>(config.getKafkaBackendStateTopic(), keyString, chunk);
      producer.send(
          record,
          (metadata, exception) -> {
            if (exception != null) {
              throw new RuntimeException(exception);
            }
          });
      ++key;
    }
    producer.flush();
  }

  private List<byte[]> chunkify(BackendState backendState) {
    try {
      String jsonString = JSON.asString(backendState);
      return chunker.chunk(jsonString.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    if (config.isDryRun()) {
      return;
    }
    producer.close();
  }
}
