package com.purbon.kafka.topology.backend.kafka;

import com.purbon.kafka.topology.backend.BackendState;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface RecordReceivedCallback {
  void apply(ConsumerRecord<String, BackendState> record);
}
