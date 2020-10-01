package com.purbon.kafka.topology.model;

import java.util.List;
import java.util.Map;

public interface Topology {

  String getContext();

  void setContext(String context);

  List<Project> getProjects();

  void addProject(Project project);

  void setProjects(List<Project> projects);

  void addOther(String fieldName, String value);

  void setPlatform(Platform platform);

  Platform getPlatform();

  Boolean isEmpty();

  Map<String, Object> asFullContext();

  List<String> getOrder();
}
