package com.purbon.kafka.topology.validation;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topology;

public interface TopologyValidation extends Validation {

  void valid(Topology topology) throws ValidationException;
}
