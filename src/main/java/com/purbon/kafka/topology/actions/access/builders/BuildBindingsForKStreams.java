package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.KStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  protected void execute() throws IOException {
    if (prefix.isEmpty()) {
      String message =
          "KStream application prefix should not be empty."
              + " Please define the applicationID or allow a nonEmpty project prefix (aka everything before the topic";
      throw new IOException(message);
    }
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
