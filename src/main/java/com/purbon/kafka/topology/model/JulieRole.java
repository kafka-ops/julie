package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JulieRole {

  private String name;
  private List<JulieRoleAcl> acls;

  @JsonCreator
  public JulieRole(
      @JsonProperty("name") String name, @JsonProperty("acls") List<JulieRoleAcl> acls) {
    this.name = name;
    this.acls = acls;
  }

  public String getName() {
    return name;
  }

  public List<JulieRoleAcl> getAcls() {
    return acls;
  }

  @Override
  public String toString() {
    return "JulieRole{" + "name='" + name + '\'' + ", acls=" + acls + '}';
  }
}
