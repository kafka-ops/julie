package com.purbon.kafka.topology.actions;

import static java.util.Collections.singletonList;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;

public class SetAclsForProducer extends BaseAccessControlAction {

  private final AccessControlProvider controlProvider;
  private final Producer producer;
  private final String fullTopicName;

  public SetAclsForProducer(
      AccessControlProvider controlProvider, Producer producer, String fullTopicName) {
    super();
    this.controlProvider = controlProvider;
    this.producer = producer;
    this.fullTopicName = fullTopicName;
  }

  @Override
  public void run() throws IOException {
    bindings =
        controlProvider.setAclsForProducers(singletonList(producer.getPrincipal()), fullTopicName);
  }
}
