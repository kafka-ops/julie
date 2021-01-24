package kafka.ops.topology.validation;

import kafka.ops.topology.exceptions.ValidationException;
import kafka.ops.topology.model.Topic;

public interface TopicValidation extends Validation {

  void valid(Topic topic) throws ValidationException;
}
