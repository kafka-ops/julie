package com.purbon.kafka.topology.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topology {

  private String team;
  private String source;

  private Map<String, String> others;

  private List<Project> projects;

  public Topology() {
    this.team = "default";
    this.source = "default";
    this.others = new HashMap<>();
    this.projects = new ArrayList<>();
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

  public void addProject(Project project) {
    project.setTopology(this);
    this.projects.add(project);
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }

  public String buildNamePrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append(getTeam())
        .append(".")
        .append(getSource());
    for(String key : others.keySet()) {
      String value = others.get(key);
      sb.append(".");
      sb.append(value);
    }
    return sb.toString();
  }

  public void addDynamicAttrs(Map<String, String> others) {
    for(String key : others.keySet()) {
      this.others.put(key, others.get(key));
    }
  }
}
