package kafka.ops.topology.actions.access.builders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kafka.ops.topology.BindingsBuilderProvider;
import kafka.ops.topology.actions.BaseAccessControlAction;
import kafka.ops.topology.model.users.KStream;

public class BuildBindingsForKStreams extends BaseAccessControlAction {

  private final BindingsBuilderProvider builderProvider;
  private final KStream app;
  private final String prefix;

  public BuildBindingsForKStreams(
      BindingsBuilderProvider builderProvider, KStream app, String topicPrefix) {
    super();
    this.builderProvider = builderProvider;
    this.app = app;
    this.prefix = app.getApplicationId().orElse(topicPrefix);
  }

  @Override
  protected void execute() {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);

    bindings =
        builderProvider.buildBindingsForStreamsApp(
            app.getPrincipal(), prefix, readTopics, writeTopics);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", app.getPrincipal());
    map.put("Topic", prefix);
    return map;
  }
}
