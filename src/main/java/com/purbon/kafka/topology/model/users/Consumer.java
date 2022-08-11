package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles;
import java.util.Objects;
import java.util.Optional;

public class Consumer extends User {

  private Optional<String> group;

  @JsonProperty(value = "group-role")
  private String groupRole = RBACPredefinedRoles.RESOURCE_OWNER;

  public Consumer() {
    super();
    group = Optional.empty();
  }

  public Consumer(String principal) {
    this(principal, null);
  }

  public Consumer(String principal, String group) {
    super(principal);
    this.group = Optional.ofNullable(group);
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

  public String getGroupRole() {
    return groupRole;
  }

  public void setGroupRole(String groupRole) {
    this.groupRole = groupRole;
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
        && groupRole.equals(consumer.getGroupRole())
        && groupString().equals(consumer.groupString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupString(), getPrincipal(), groupRole);
  }
}
