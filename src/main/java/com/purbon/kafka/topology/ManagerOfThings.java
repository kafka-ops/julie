package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;

public interface ManagerOfThings {

  void apply(Topology topology, ExecutionPlan plan) throws IOException;
}
