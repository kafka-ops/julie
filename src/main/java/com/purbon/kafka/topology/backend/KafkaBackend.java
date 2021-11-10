package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendConsumer;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendProducer;
import com.purbon.kafka.topology.backend.kafka.RecordReceivedCallback;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaBackend implements Backend, RecordReceivedCallback {

  private static final Logger LOGGER = LogManager.getLogger(KafkaBackend.class);

  private boolean isCompleted;

  private KafkaBackendConsumer consumer;
  private KafkaBackendProducer producer;

  private AtomicReference<BackendState> latest;
  private String instanceId;
  private Thread thread;

  public KafkaBackend() {
    isCompleted = false;
  }

  private static class JulieKafkaConsumerThread implements Runnable {
    private KafkaBackend callback;
    private KafkaBackendConsumer consumer;

    public JulieKafkaConsumerThread(KafkaBackend callback, KafkaBackendConsumer consumer) {
      this.callback = callback;
      this.consumer = consumer;
    }

    public void run() {
      consumer.start();
      try {
        consumer.retrieve(callback);
      } catch (WakeupException ex) {
        LOGGER.trace(ex);
      }
    }
  }

  @SneakyThrows
  @Override
  public void configure(Configuration config) {
    instanceId = config.getJulieInstanceId();
    latest = new AtomicReference<>(new BackendState());

    consumer = new KafkaBackendConsumer(config);
    consumer.configure();

    var topics = consumer.listTopics();
    if (!topics.containsKey(config.getJulieKafkaConfigTopic())) {
      throw new IOException(
          "The internal julie kafka configuration topic topic "
              + config.getJulieKafkaConfigTopic()
              + " should exist in the cluster");
    }
    producer = new KafkaBackendProducer(config);
    producer.configure();

    thread = new Thread(new JulieKafkaConsumerThread(this, consumer), "kafkaJulieConsumer");
    thread.start();
    waitForCompletion();
  }

  public synchronized void waitForCompletion() throws InterruptedException {
    while (!isCompleted) {
      wait(30000);
    }
  }

  public synchronized void complete() {
    isCompleted = true;
    notify();
  }

  @Override
  public void save(BackendState state) throws IOException {
    producer.save(state);
  }

  @Override
  public BackendState load() throws IOException {
    return latest == null ? new BackendState() : latest.get();
  }

  @Override
  public void close() {
    consumer.stop();
    producer.stop();
    try {
      thread.join();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    }
    latest = null;
    thread = null;
  }

  @Override
  public void apply(ConsumerRecord<String, BackendState> record) {
    if (instanceId.equals(record.key()) && latest != null) {
      latest.set(record.value());
    }
  }
}
