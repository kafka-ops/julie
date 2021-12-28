package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.BindingsBuilderProvider;
import com.purbon.kafka.topology.model.users.Consumer;
import java.util.List;

public class ConsumerAclBindingsBuilder implements AclBindingsBuilder {

  private final String fullTopicName;
  private final List<Consumer> consumers;
  private final BindingsBuilderProvider builderProvider;
  private boolean prefixed;

  public ConsumerAclBindingsBuilder(
      BindingsBuilderProvider builderProvider,
      List<Consumer> consumers,
      String fullTopicName,
      boolean prefixed) {
    this.consumers = consumers;
    this.fullTopicName = fullTopicName;
    this.builderProvider = builderProvider;
    this.prefixed = prefixed;
  }

  @Override
  public AclBindingsResult getAclBindings() {
    return AclBindingsResult.forAclBindings(
        builderProvider.buildBindingsForConsumers(consumers, fullTopicName, prefixed));
  }
}
