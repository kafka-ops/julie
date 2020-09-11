package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.ClearAcls;
import com.purbon.kafka.topology.actions.CreateBindings;
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
  private List<TopologyAclBinding> bindings;
  private boolean allowDelete;

  public ExecutionPlan(List<Action> plan, PrintStream outputStream, boolean allowDelete) {
    this.plan = plan;
    this.outputStream = outputStream;
    this.bindings = new ArrayList<>();
    this.allowDelete = allowDelete;
  }

  public ExecutionPlan() {
    this(new ArrayList<>(), System.out, false);
  }

  public void add(Action action) {
    this.plan.add(action);
  }

  public void init(ClusterState clusterState, Boolean allowDelete, PrintStream outputStream)
      throws IOException {
    clusterState.load();
    this.clusterState = clusterState;
    this.plan.clear();
    this.bindings.clear();
    this.allowDelete = allowDelete;
    this.outputStream = outputStream;
  }

  public void run(boolean dryRun, AccessControlProvider controlProvider) {
    Set<TopologyAclBinding> bindings = new HashSet<>();

    for (Action action : plan) {
      if (dryRun) {
        outputStream.println(action);
      } else {
        try {
          action.run();
          if (!action.getBindings().isEmpty()) {
            bindings.addAll(action.getBindings());
          }
        } catch (IOException ex) {
          LOGGER.error(ex);
        }
      }
    }

    CreateBindings createBindings = new CreateBindings(controlProvider, bindings);
    try {
      execute(createBindings, dryRun);
    } catch (IOException e) {
      LOGGER.error(e);
    }

    if (allowDelete) {
      // clear acls that does not appear anymore in the new generated list,
      // but where previously created
      Set<TopologyAclBinding> bindingsToDelete =
          clusterState.getBindings().stream()
              .filter(binding -> !bindings.contains(binding))
              .collect(Collectors.toSet());

      ClearAcls clearAcls = new ClearAcls(controlProvider, bindingsToDelete);

      try {
        execute(clearAcls, dryRun);
        clusterState.reset();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }

    clusterState.add(new ArrayList<>(bindings));
    clusterState.flushAndClose();
  }

  private void execute(Action action, boolean dryRun) throws IOException {
    if (dryRun) {
      outputStream.println(action);
    } else {
      action.run();
    }
  }

  public List<TopologyAclBinding> getBindings() {
    return bindings;
  }
}
