package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Other extends User {

  private Map<String, Object> unmappedFields = new HashMap<>();

  private Optional<String> transactionId;
  private Optional<Boolean> idempotence;
  private Optional<String> group;
  private Optional<String> topic;
  private Optional<String> subject;
  private Optional<String> connector;

  public Other() {
    super();
    topic = Optional.empty();
    subject = Optional.empty();
    connector = Optional.empty();
    group = Optional.empty();
    transactionId = Optional.empty();
    idempotence = Optional.empty();
  }

  public Map<String, Object> asMap() {
    Map<String, Object> map = new HashMap<>();
    map.putAll(unmappedFields);
    map.put("topic", topicString());
    if (subject.isPresent()) {
      map.put("subject", subjectString());
    }
    if (connector.isPresent()) {
      map.put("connector", connectorString());
    }
    map.put("group", groupString());
    if (transactionId.isPresent()) {
      map.put("transactionId", transactionId.get());
    }
    return map;
  }

  // Capture all other fields that Jackson do not match other members
  @JsonAnyGetter
  public Map<String, Object> otherFields() {
    return unmappedFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, Object value) {
    unmappedFields.put(name, value);
  }

  public String groupString() {
    return group.orElse("*");
  }

  public Optional<String> getGroup() {
    return group;
  }

  public void setGroup(Optional<String> group) {
    this.group = group;
  }

  public String topicString() {
    return topic.orElse("");
  }

  public Optional<String> getTopic() {
    return topic;
  }

  public void setTopic(Optional<String> topic) {
    this.topic = topic;
  }

  public Optional<String> getSubject() {
    return subject;
  }

  public String subjectString() {
    return subject.orElse("");
  }

  public void setSubject(Optional<String> subject) {
    this.subject = subject;
  }

  public Optional<String> getConnector() {
    return connector;
  }

  public String connectorString() {
    return connector.orElse("");
  }

  public void setConnector(Optional<String> connector) {
    this.connector = connector;
  }

  public Optional<String> getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Optional<String> transactionId) {
    this.transactionId = transactionId;
  }

  public Optional<Boolean> getIdempotence() {
    return idempotence;
  }

  public void setIdempotence(Optional<Boolean> idempotence) {
    this.idempotence = idempotence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Other)) {
      return false;
    }
    Other other = (Other) o;
    return getPrincipal().equals(other.getPrincipal())
        && groupString().equals(other.groupString())
        && getTransactionId().equals(other.getTransactionId())
        && getIdempotence().equals(other.getIdempotence());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupString(), getPrincipal(), getTransactionId(), getIdempotence());
  }
}
