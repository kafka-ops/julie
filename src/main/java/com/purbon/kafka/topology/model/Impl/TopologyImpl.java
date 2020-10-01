package com.purbon.kafka.topology.model.Impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyImpl implements Topology, Cloneable {

  @JsonIgnore private final TopologyBuilderConfig config;

  private String context;
  @JsonIgnore private Map<String, String> others;

  private List<Project> projects;
  @JsonIgnore private List<String> order;
  private Platform platform;

  public TopologyImpl() {
    this(new TopologyBuilderConfig());
  }

  public TopologyImpl(TopologyBuilderConfig config) {
    this.context = "default";
    this.others = new HashMap<>();
    this.order = new ArrayList<>();
    this.projects = new ArrayList<>();
    this.platform = new Platform();
    this.config = config;
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
    project.setPrefixContextAndOrder(asFullContext(), getOrder());
    this.projects.add(project);
  }

  public void setProjects(List<Project> projects) {
    this.projects.clear();
    projects.forEach(this::addProject);
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
  public Map<String, Object> asFullContext() {
    Map<String, Object> context = new HashMap<>(others);
    context.put("context", getContext());
    return context;
  }

  @Override
  public List<String> getOrder() {
    return order;
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
