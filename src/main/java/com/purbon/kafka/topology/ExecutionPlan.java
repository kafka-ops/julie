package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecutionPlan {

  private static final Logger LOGGER = LogManager.getLogger(ExecutionPlan.class);

  private final List<Action> plan;
  private PrintStream outputStream;
  private List<TopologyAclBinding> bindings;

  public ExecutionPlan(List<Action> plan, PrintStream outputStream) {
    this.plan = plan;
    this.outputStream = outputStream;
    this.bindings = new ArrayList<>();
  }

  public ExecutionPlan() {
    this(new ArrayList<>(), System.out);
  }

  public void add(Action action) {
    this.plan.add(action);
  }

  public void start() {
    this.plan.clear();
    this.bindings.clear();
  }

  public void run(boolean dryRun) {
    bindings = new ArrayList<>();

    try {
      for (Action action : plan) {
        if (dryRun) {
          outputStream.println(action);
        } else {
          action.run();
          if (!action.getBindings().isEmpty()) {
            bindings.addAll(action.getBindings());
          }
        }
      }
    } catch (IOException ex) {
      LOGGER.error("something happened during the execution of actions", ex);
    }
  }

  public List<TopologyAclBinding> getBindings() {
    return bindings;
  }
}
