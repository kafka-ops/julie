package kafka.ops.topology.actions.access.builders;

import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.model.users.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildBindingsForConsumer extends BaseAccessControlAction {

  private final String fullTopicName;
  private final List<Consumer> consumers;
  private final BindingsBuilderProvider builderProvider;
  private boolean prefixed;

  public BuildBindingsForConsumer(
      BindingsBuilderProvider builderProvider,
      List<Consumer> consumers,
      String fullTopicName,
      boolean prefixed) {
    super();
    this.consumers = consumers;
    this.fullTopicName = fullTopicName;
    this.builderProvider = builderProvider;
    this.prefixed = prefixed;
  }

  @Override
  protected void execute() {
    bindings = builderProvider.buildBindingsForConsumers(consumers, fullTopicName, prefixed);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", consumers);
    map.put("Topic", fullTopicName);
    return map;
  }
}
