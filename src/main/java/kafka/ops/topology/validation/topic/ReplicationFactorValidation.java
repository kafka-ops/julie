package kafka.ops.topology.validation.topic;

import kafka.ops.topology.exceptions.ValidationException;
import kafka.ops.topology.model.Topic;
import kafka.ops.topology.validation.TopicValidation;

public class ReplicationFactorValidation implements TopicValidation {

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.replicationFactor() != 3) {
      String msg =
          String.format(
              "Topic %s has an unexpected replication factor: %s",
              topic, topic.replicationFactor());
      throw new ValidationException(msg);
    }
  }
}
