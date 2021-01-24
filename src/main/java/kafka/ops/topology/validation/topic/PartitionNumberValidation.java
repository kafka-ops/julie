package kafka.ops.topology.validation.topic;

import kafka.ops.topology.exceptions.ValidationException;
import kafka.ops.topology.model.Topic;
import kafka.ops.topology.validation.TopicValidation;

public class PartitionNumberValidation implements TopicValidation {

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.partitionsCount() < 3) {
      String msg =
          String.format(
              "Topic %s has an invalid number of partitions: %s", topic, topic.partitionsCount());
      throw new ValidationException(msg);
    }
  }
}
