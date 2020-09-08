package com.purbon.kafka.topology.model.users.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.User;
import java.util.Optional;

public class SchemaRegistryInstance extends User {

  private static final String DEFAULT_SCHEMA_TOPIC = "_schemas";

  private static final String DEFAULT_SCHEMA_REGISTRY_GROUP = "schema-registry";

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> topic;

  private Optional<String> group;

  public SchemaRegistryInstance() {
    this("");
  }

  public SchemaRegistryInstance(String principal) {
    this(principal, Optional.empty(), Optional.empty());
  }

  public SchemaRegistryInstance(String principal, Optional<String> topic, Optional<String> group) {
    super(principal);
    this.topic = topic;
    this.group = group;
  }

  public String topicString() {
    return topic.orElse(DEFAULT_SCHEMA_TOPIC);
  }

  public void setTopic(Optional<String> topic) {
    this.topic = topic;
  }

  public String groupString() {
    return group.orElse(DEFAULT_SCHEMA_REGISTRY_GROUP);
  }

  public void setGroup(Optional<String> group) {
    this.group = group;
  }

  public Optional<String> getTopic() {
    return topic;
  }

  public Optional<String> getGroup() {
    return group;
  }
}
