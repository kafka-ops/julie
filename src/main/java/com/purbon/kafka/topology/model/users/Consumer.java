package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.Objects;
import java.util.Optional;

public class Consumer extends User {

  private Optional<String> group;

  public Consumer() {
    super();
    group = Optional.empty();
  }

  public Consumer(String principal) {
    super(principal);
    group = Optional.empty();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Consumer)) {
      return false;
    }
    Consumer consumer = (Consumer) o;
    return getPrincipal().equals(consumer.getPrincipal())
        && groupString().equals(consumer.groupString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupString(), getPrincipal());
  }
}
