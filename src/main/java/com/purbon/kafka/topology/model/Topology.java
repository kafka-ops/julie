package com.purbon.kafka.topology.model;

import java.util.List;

public class Topology {

  private String team;
  private String source;

  private List<Project> projects;

  public Topology() {

  }

  public String getTeam() {
    return team;
  }

  public void setTeam(String team) {
    this.team = team;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public List<Project> getProjects() {
    return projects;
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }
}
