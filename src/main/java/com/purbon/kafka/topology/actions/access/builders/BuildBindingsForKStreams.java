package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.actions.BaseAccessControlAction;
import com.purbon.kafka.topology.model.users.KStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildBindingsForKStreams extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final KStream app;
  private final String topicPrefix;

  public BuildBindingsForKStreams(
      AccessControlProvider controlProvider, KStream app, String topicPrefix) {
    super();
    this.controlProvider = controlProvider;
    this.app = app;
    this.topicPrefix = topicPrefix;
  }

  @Override
  public void run() throws IOException {
    List<String> readTopics = app.getTopics().get(KStream.READ_TOPICS);
    List<String> writeTopics = app.getTopics().get(KStream.WRITE_TOPICS);

    bindings =
        controlProvider.setAclsForStreamsApp(
            app.getPrincipal(), topicPrefix, readTopics, writeTopics);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principal", app.getPrincipal());
    map.put("Topic", topicPrefix);
    return map;
  }
}
