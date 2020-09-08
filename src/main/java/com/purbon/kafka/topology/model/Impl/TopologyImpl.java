package com.purbon.kafka.topology.model.Impl;

import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyImpl implements Topology, Cloneable {

  private String context;

  private Map<String, String> others;

  private List<Project> projects;
  private List<String> order;
  private Platform platform;

  public TopologyImpl() {
    this.context = "default";
    this.others = new HashMap<>();
    this.order = new ArrayList<>();
    this.projects = new ArrayList<>();
    this.platform = new Platform();
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public List<Project> getProjects() {
    return projects;
  }

  public void addProject(Project project) {
    project.setTopologyPrefix(buildNamePrefix());
    this.projects.add(project);
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }

  public String buildNamePrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append(getContext());
    for (String key : order) {
      String value = others.get(key);
      sb.append(".");
      sb.append(value);
    }
    return sb.toString();
  }

  public void addOther(String fieldName, String value) {
    order.add(fieldName);
    others.put(fieldName, value);
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public Platform getPlatform() {
    return this.platform;
  }

  public Boolean isEmpty() {
    return context.isEmpty();
  }

  @Override
  public Topology clone() {
    try {
      return (Topology) super.clone();
    } catch (CloneNotSupportedException e) {
      Topology topology = new TopologyImpl();
      topology.setContext(getContext());
      topology.setPlatform(getPlatform());
      topology.setProjects(getProjects());
      return topology;
    }
  }
}
