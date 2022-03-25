package com.purbon.kafka.topology.validation.topic;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@RequiredArgsConstructor
public class PartitionNumberValidation implements TopicValidation {

  @NonNull private Configuration config;

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.getPartitionCount().isPresent() && topic.partitionsCount() < 3) {
      String msg =
          String.format(
              "Topic %s has an invalid number of partitions: %s", topic, topic.partitionsCount());
      throw new ValidationException(msg);
    }
  }
}
