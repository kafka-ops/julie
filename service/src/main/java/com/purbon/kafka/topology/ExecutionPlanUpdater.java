package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

public interface ExecutionPlanUpdater {

  default void updatePlan(Topology topology, ExecutionPlan plan) throws IOException {
    updatePlan(plan, Collections.singletonMap(topology.getContext(), topology));
  }

  void updatePlan(ExecutionPlan plan, Map<String, Topology> topologies) throws IOException;

  void printCurrentState(PrintStream out) throws IOException;
}
