package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Other extends User {

  private Optional<String> transactionId;
  private Optional<Boolean> idempotence;
  private Optional<String> group;
  private Optional<String> topic;

  public Other() {
    super();
    topic = Optional.empty();
    group = Optional.empty();
    transactionId = Optional.empty();
    idempotence = Optional.empty();
  }

  public Map<String, Object> asMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("topic", topicString());
    map.put("group", groupString());
    if (transactionId.isPresent()) {
      map.put("transactionId", transactionId.get());
    }
    return map;
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
