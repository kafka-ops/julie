package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

public interface ManagerOfThings {

  default void apply(Topology topology, ExecutionPlan plan) throws IOException {
    apply(Collections.singletonMap(topology.getContext(), topology), plan);
  }

  void apply(Map<String, Topology> topologies, ExecutionPlan plan) throws IOException;

  void printCurrentState(PrintStream out) throws IOException;
}
