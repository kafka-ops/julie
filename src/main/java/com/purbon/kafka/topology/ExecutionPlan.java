package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.Action;
import com.purbon.kafka.topology.actions.access.ClearBindings;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecutionPlan {

  private static final Logger LOGGER = LogManager.getLogger(ExecutionPlan.class);

  private final List<Action> plan;
  private PrintStream outputStream;
  private BackendController backendController;
  private Set<TopologyAclBinding> bindings;

  private ExecutionPlan(
      List<Action> plan, PrintStream outputStream, BackendController backendController) {
    this.plan = plan;
    this.outputStream = outputStream;
    this.bindings = new LinkedHashSet<>();
    this.backendController = backendController;
    if (backendController.size() > 0) {
      this.bindings.addAll(backendController.getBindings());
    }
  }

  public void add(Action action) {
    this.plan.add(action);
  }

  public static ExecutionPlan init(BackendController backendController, PrintStream outputStream)
      throws IOException {
    backendController.load();
    ExecutionPlan plan = new ExecutionPlan(new ArrayList<>(), outputStream, backendController);
    return plan;
  }

  public void run() throws IOException {
    run(false);
  }

  public void run(boolean dryRun) throws IOException {
    for (Action action : plan) {
      try {
        execute(action, dryRun);
      } catch (IOException e) {
        LOGGER.error(String.format("Something happen running action %s", action), e);
        throw e;
      }
    }

    backendController.reset();
    backendController.add(new ArrayList<>(bindings));
    backendController.flushAndClose();
  }

  private void execute(Action action, boolean dryRun) throws IOException {
    LOGGER.debug(String.format("Execution action %s (dryRun=%s)", action, dryRun));
    if (dryRun) {
      outputStream.println(action);
    } else {
      action.run();
      if (!action.getBindings().isEmpty()) {
        if (action instanceof ClearBindings) {
          bindings =
              bindings.stream()
                  .filter(binding -> !action.getBindings().contains(binding))
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
