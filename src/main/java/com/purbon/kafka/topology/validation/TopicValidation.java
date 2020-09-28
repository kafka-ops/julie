package com.purbon.kafka.topology.validation;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;

public interface TopicValidation extends Validation {

  void valid(Topic topic) throws ValidationException;
}
