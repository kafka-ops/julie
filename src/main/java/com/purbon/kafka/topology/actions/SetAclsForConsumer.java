package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Consumer;
import java.io.IOException;
import java.util.Collections;

public class SetAclsForConsumer extends BaseAccessControlAction {

  private final String fullTopicName;
  private final Consumer consumer;
  private final AccessControlProvider controlProvider;

  public SetAclsForConsumer(
      AccessControlProvider controlProvider, Consumer consumer, String fullTopicName) {
    super();
    this.consumer = consumer;
    this.fullTopicName = fullTopicName;
    this.controlProvider = controlProvider;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setAclsForConsumers(Collections.singletonList(consumer), fullTopicName);
  }
}
