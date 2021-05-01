package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topology;
import java.io.IOException;
import java.io.PrintStream;

public interface ManagerOfThings {

  void apply(Topology topology, ExecutionPlan plan) throws IOException;

  void printCurrentState(PrintStream out) throws IOException;
}
