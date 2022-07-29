package com.purbon.kafka.topology.backend.kafka;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.BackendState;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaBackendProducer {

  private static final Logger LOGGER = LogManager.getLogger(KafkaBackendProducer.class);

  private String instanceId;
  private Configuration config;
  private KafkaProducer<String, BackendState> producer;
  private Future<RecordMetadata> future;

  public KafkaBackendProducer(Configuration config) {
    this.config = config;
    this.instanceId = config.getJulieInstanceId();
  }

  public void configure() {
    Properties props = config.asProperties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass());
    var serde = new JsonSerializer<BackendState>();
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serde.getClass());
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    producer = new KafkaProducer<>(props);
    future = null;
  }

  public void save(BackendState backendState) {
    var record = new ProducerRecord<>(config.getJulieKafkaConfigTopic(), instanceId, backendState);
    future =
        producer.send(
            record,
            (recordMetadata, e) -> {
              if (e != null) {
                LOGGER.error(e);
              }
              LOGGER.info("RecordAckd: metadata=" + recordMetadata.offset());
            });
    try {
      future.get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    if (future != null) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error(e);
      }
    }
    producer.close();
  }
}
