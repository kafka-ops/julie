package com.purbon.kafka.topology.validation.topic;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;

public class ReplicationFactorValidation implements TopicValidation {

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.replicationFactor().isPresent() && topic.replicationFactor().get() != 3) {
      String msg =
          String.format(
              "Topic %s has an unexpected replication factor: %s",
              topic, topic.replicationFactor().get());
      throw new ValidationException(msg);
    }
  }
}
