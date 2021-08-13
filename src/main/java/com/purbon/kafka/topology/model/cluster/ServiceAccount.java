package com.purbon.kafka.topology.model.cluster;

import java.util.Objects;

public class ServiceAccount {

  private int id;
  private String name;
  private String description;
  private String resourceId;

  public ServiceAccount() {
    this(-1, "", "", "");
  }

  public ServiceAccount(int id, String name, String description) {
    this(id, name, description, "");
  }

  public ServiceAccount(int id, String name, String description, String resourceId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.resourceId = resourceId;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getResourceId() {
    return resourceId;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("ServiceAccount{");
    sb.append("id=").append(id);
    sb.append(", name='").append(name).append('\'');
    sb.append(", resourceId='").append(resourceId).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceAccount)) {
      return false;
    }
    ServiceAccount that = (ServiceAccount) o;
    return getName().equals(that.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getDescription(), getResourceId());
  }
}
