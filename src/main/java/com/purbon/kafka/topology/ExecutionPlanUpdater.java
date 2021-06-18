package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;

public interface ExecutionPlanUpdater {

  void updatePlan(ExecutionPlan plan, Topology topology) throws IOException;

  void printCurrentState(PrintStream out) throws IOException;
}
