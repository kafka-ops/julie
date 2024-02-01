package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendConsumer;
import com.purbon.kafka.topology.backend.kafka.KafkaBackendProducer;
import com.purbon.kafka.topology.backend.kafka.RecordReceivedCallback;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class KafkaBackend implements Backend, RecordReceivedCallback {

  private static final Logger LOGGER = LogManager.getLogger(KafkaBackend.class);

  private boolean isCompleted;

  private KafkaBackendConsumer consumer;
  private KafkaBackendProducer producer;

  private AtomicReference<BackendState> latest;
  private AtomicBoolean shouldWaitForLoad;
  private String instanceId;
  private Thread thread;

  public KafkaBackend() {
    isCompleted = false;
    shouldWaitForLoad = new AtomicBoolean(true);
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
    waitForLoad();
    consumer = createConsumer(config);
    consumer.configure();

    var topics = consumer.listTopics();
    if (!topics.containsKey(config.getJulieKafkaConfigTopic())) {
      throw new IOException(
          "The internal julie kafka configuration topic topic "
              + config.getJulieKafkaConfigTopic()
              + " should exist in the cluster");
    }
    producer = createProducer(config);
    producer.configure();

    thread = createThread();
    thread.start();
    waitForCompletion();
  }

  void waitForLoad() {
    shouldWaitForLoad.set(true);
  }

  @NotNull
  Thread createThread() {
    return new Thread(new JulieKafkaConsumerThread(this, consumer), "kafkaJulieConsumer");
  }

  @NotNull
  KafkaBackendProducer createProducer(Configuration config) {
    return new KafkaBackendProducer(config);
  }

  @NotNull
  KafkaBackendConsumer createConsumer(Configuration config) {
    return new KafkaBackendConsumer(config);
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

  @SneakyThrows
  @Override
  public BackendState load() throws IOException {
    while (shouldWaitForLoad.get()) {
      continue;
    }
    return latest == null ? new BackendState() : latest.get();
  }

  public void initialLoadFinish() {
    shouldWaitForLoad.set(false);
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
