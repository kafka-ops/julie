package com.purbon.kafka.topology.model.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ServiceAccount {

  private String id;
  private String name;
  private String description;

  @JsonProperty("resource_id")
  private String resourceId;

  public ServiceAccount() {
    this("", "", "", "");
  }

  public ServiceAccount(String id, String name, String description) {
    this(id, name, description, "");
  }

  public ServiceAccount(String id, String name, String description, String resourceId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.resourceId = resourceId;
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
