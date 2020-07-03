package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.User;
import java.util.Optional;

public class SchemaRegistry extends User {

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> topic;

  private Optional<String> group;

  public SchemaRegistry() {
    this("");
  }

  public SchemaRegistry(String principal) {
    this(principal, Optional.empty(), Optional.empty());
  }

  public SchemaRegistry(String principal, Optional<String> topic, Optional<String> group) {
    super(principal);
    this.topic = topic;
    this.group = group;
  }

  public String getTopic() {
    return topic.orElse("_schemas");
  }

  public void setTopic(Optional<String> topic) {
    this.topic = topic;
  }

  public String getGroup() {
    return group.orElse("schema-registry");
  }

  public void setGroup(Optional<String> group) {
    this.group = group;
  }
}
