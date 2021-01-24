package kafka.ops.topology.validation;

import kafka.ops.topology.exceptions.ValidationException;
import kafka.ops.topology.model.Topology;

public interface TopologyValidation extends Validation {

  void valid(Topology topology) throws ValidationException;
}
