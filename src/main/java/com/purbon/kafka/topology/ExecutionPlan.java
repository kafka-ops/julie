package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.ClearAcls;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecutionPlan {

  private static final Logger LOGGER = LogManager.getLogger(ExecutionPlan.class);

  private final List<Action> plan;
  private PrintStream outputStream;
  private ClusterState clusterState;
  private Set<TopologyAclBinding> bindings;

  public ExecutionPlan(List<Action> plan, PrintStream outputStream, ClusterState clusterState) {
    this.plan = plan;
    this.outputStream = outputStream;
    this.bindings = new HashSet<>();
    this.clusterState = clusterState;
  }

  public ExecutionPlan() {
    this(new ArrayList<>(), System.out, new ClusterState());
  }

  public void add(Action action) {
    this.plan.add(action);
  }

  public void init(ClusterState clusterState, Boolean allowDelete, PrintStream outputStream)
      throws IOException {
    this.clusterState = clusterState;
    this.clusterState.load();
    this.plan.clear();
    this.bindings.clear();
    this.outputStream = outputStream;
  }

  public void run() throws IOException {
    run(false);
  }

  public void run(boolean dryRun) throws IOException {
    for (Action action : plan) {
      try {
        execute(action, dryRun);
      } catch (IOException e) {
        LOGGER.error(e.getCause());
        throw e;
      }
    }

    clusterState.reset();
    clusterState.add(new ArrayList<>(bindings));
    clusterState.flushAndClose();
  }

  private void execute(Action action, boolean dryRun) throws IOException {
    if (dryRun) {
      outputStream.println(action);
    } else {
      action.run();
      if (!action.getBindings().isEmpty()) {
        if (action instanceof ClearAcls) {
          bindings =
              bindings.stream()
                  .filter(binding -> action.getBindings().contains(binding))
                  .collect(Collectors.toSet());
        } else {
          bindings.addAll(action.getBindings());
        }
      }
    }
  }

  public Set<TopologyAclBinding> getBindings() {
    return bindings;
  }

  public List<Action> getActions() {
    return plan;
  }
}
